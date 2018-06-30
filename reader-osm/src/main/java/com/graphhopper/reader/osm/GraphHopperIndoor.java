/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.reader.osm;

import com.graphhopper.*;
import com.graphhopper.reader.DataReader;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.QueryGraph;
import com.graphhopper.routing.RoutingAlgorithmFactory;
import com.graphhopper.routing.ch.PrepareContractionHierarchies;
import com.graphhopper.routing.lm.LMAlgoFactoryDecorator;
import com.graphhopper.routing.template.AlternativeRoutingTemplate;
import com.graphhopper.routing.template.RoutingTemplateIndoor;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.HintsMap;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.*;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.*;
import com.graphhopper.util.exceptions.PointOutOfBoundsException;
import com.graphhopper.util.shapes.BBox;
import com.graphhopper.util.shapes.GHPointIndoor;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.graphhopper.util.Parameters.Algorithms.*;


/**
 * Easy to use access point to configure import and (offline) routing.
 *
 * @author Peter Karich
 * @see GraphHopperAPI
 *
 * Adapted to Indoor Navigation
 */
public class GraphHopperIndoor extends GraphHopper {
    private GraphHopperStorage ghStorage;
    private boolean fullyLoaded = false;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private boolean simplifyResponse = true;


    public GraphHopperIndoor() {
        super();
    }

    @Override
    protected DataReader createReader(GraphHopperStorage ghStorage) {
        return initDataReader(new OSMReaderIndoor(ghStorage));
    }

    @Override
    public void setGraphHopperStorage(GraphHopperStorage ghStorage) {
        this.ghStorage = ghStorage;
        this.fullyLoaded = true;
    }


    public List<Path> calcPaths(GHRequestIndoor request, GHResponse ghRsp) {
        setGraphHopperStorage(super.getGraphHopperStorage());
        if (ghStorage == null || !fullyLoaded)
            throw new IllegalStateException("Do a successful call to load or importOrLoad before routing");

        if (ghStorage.isClosed())
            throw new IllegalStateException("You need to create a new GraphHopper instance as it is already closed");

        // default handling
        String vehicle = request.getVehicle();
        if (vehicle.isEmpty()) {
            vehicle = "indoor";
        }

        Lock readLock = readWriteLock.readLock();
        readLock.lock();
        try {
            if (!super.getEncodingManager().supports(vehicle))
                throw new IllegalArgumentException("Vehicle " + vehicle + " unsupported. "
                        + "Supported are: " + getEncodingManager());

            HintsMap hints = request.getHints();
            String tModeStr = hints.get("traversal_mode", super.getTraversalMode().toString());
            TraversalMode tMode = TraversalMode.fromString(tModeStr);
            if (hints.has(Parameters.Routing.EDGE_BASED))
                tMode = hints.getBool(Parameters.Routing.EDGE_BASED, false) ? TraversalMode.EDGE_BASED_2DIR : TraversalMode.NODE_BASED;

            FlagEncoder encoder = super.getEncodingManager().getEncoder(vehicle);

            boolean disableCH = hints.getBool(Parameters.CH.DISABLE, false);
            if (!super.getCHFactoryDecorator().isDisablingAllowed() && disableCH)
                throw new IllegalArgumentException("Disabling CH not allowed on the server-side");

            boolean disableLM = hints.getBool(Parameters.Landmark.DISABLE, false);
            if (!super.getLMFactoryDecorator().isDisablingAllowed() && disableLM)
                throw new IllegalArgumentException("Disabling LM not allowed on the server-side");

            String algoStr = request.getAlgorithm();
            if (algoStr.isEmpty())
                algoStr = super.getCHFactoryDecorator().isEnabled() && !disableCH ? DIJKSTRA_BI : ASTAR_BI;

            List<GHPointIndoor> points = request.getIndoorPoints();
            // TODO Maybe we should think about a isRequestValid method that checks all that stuff that we could do to fail fast
            // For example see #734
            checkIfPointsAreInBounds(points);

            RoutingTemplateIndoor routingTemplate = new RoutingTemplateIndoor(request, ghRsp, super.getLocationIndex());

            List<Path> altPaths = null;
            int maxRetries = routingTemplate.getMaxRetries();
            Locale locale = request.getLocale();
            Translation tr = super.getTranslationMap().getWithFallBack(locale);
            for (int i = 0; i < maxRetries; i++) {
                StopWatch sw = new StopWatch().start();
                List<QueryResult> qResults = routingTemplate.lookupIndoor(points, encoder);
                ghRsp.addDebugInfo("idLookup:" + sw.stop().getSeconds() + "s");
                if (ghRsp.hasErrors())
                    return Collections.emptyList();

                RoutingAlgorithmFactory tmpAlgoFactory = getAlgorithmFactory(hints);
                Weighting weighting;
                QueryGraph queryGraph;

                if (super.getCHFactoryDecorator().isEnabled() && !disableCH) {
                    boolean forceCHHeading = hints.getBool(Parameters.CH.FORCE_HEADING, false);
                    if (!forceCHHeading && request.hasFavoredHeading(0))
                        throw new IllegalArgumentException("Heading is not (fully) supported for CHGraph. See issue #483");

                    // if LM is enabled we have the LMFactory with the CH algo!
                    RoutingAlgorithmFactory chAlgoFactory = tmpAlgoFactory;
                    if (tmpAlgoFactory instanceof LMAlgoFactoryDecorator.LMRAFactory)
                        chAlgoFactory = ((LMAlgoFactoryDecorator.LMRAFactory) tmpAlgoFactory).getDefaultAlgoFactory();

                    if (chAlgoFactory instanceof PrepareContractionHierarchies)
                        weighting = ((PrepareContractionHierarchies) chAlgoFactory).getWeighting();
                    else
                        throw new IllegalStateException("Although CH was enabled a non-CH algorithm factory was returned " + tmpAlgoFactory);

                    tMode = getCHFactoryDecorator().getNodeBase();
                    queryGraph = new QueryGraph(ghStorage.getGraph(CHGraph.class, weighting));
                    queryGraph.lookup(qResults);
                } else {
                    queryGraph = new QueryGraph(ghStorage);
                    queryGraph.lookup(qResults);
                    weighting = createWeighting(hints, encoder, queryGraph);
                    ghRsp.addDebugInfo("tmode:" + tMode.toString());
                }

                int maxVisitedNodesForRequest = hints.getInt(Parameters.Routing.MAX_VISITED_NODES, super.getMaxVisitedNodes());
                if (maxVisitedNodesForRequest > super.getMaxVisitedNodes())
                    throw new IllegalArgumentException("The max_visited_nodes parameter has to be below or equal to:" + super.getMaxVisitedNodes());

                weighting = createTurnWeighting(queryGraph, weighting, tMode);

                AlgorithmOptions algoOpts = AlgorithmOptions.start().
                        algorithm(algoStr).traversalMode(tMode).weighting(weighting).
                        maxVisitedNodes(maxVisitedNodesForRequest).
                        hints(hints).
                        build();

                altPaths = routingTemplate.calcPaths(queryGraph, tmpAlgoFactory, algoOpts);

                boolean tmpEnableInstructions = hints.getBool(Parameters.Routing.INSTRUCTIONS, super.isEnableInstructions());
                double wayPointMaxDistance = hints.getDouble(Parameters.Routing.WAY_POINT_MAX_DISTANCE, 1d);

                DouglasPeucker peucker = new DouglasPeucker().setMaxDistance(wayPointMaxDistance);
                PathMerger pathMerger = new PathMerger().
                        setCalcPoints(true).
                        setDouglasPeucker(peucker).
                        setEnableInstructions(tmpEnableInstructions).
                        setPathDetailsBuilders(getPathDetailsBuilderFactory(), request.getPathDetails()).
                        setSimplifyResponse(simplifyResponse && wayPointMaxDistance > 0);

                if (request.hasFavoredHeading(0))
                    pathMerger.setFavoredHeading(request.getFavoredHeading(0));

                if (routingTemplate.isReady(pathMerger, tr))
                    break;
            }

            return altPaths;

        } catch (IllegalArgumentException ex) {
            ghRsp.addError(ex);
            return Collections.emptyList();
        } finally {
            readLock.unlock();
        }
    }

    private void checkIfPointsAreInBounds(List<GHPointIndoor> points) {
        BBox bounds = getGraphHopperStorage().getBounds();
        for (int i = 0; i < points.size(); i++) {
            GHPointIndoor point = points.get(i);
            if (!bounds.contains(point.getLat(), point.getLon())) {
                throw new PointOutOfBoundsException("Point " + i + " is out of bounds: " + point, i);
            }
        }
    }

    /**
     * This method specifies if the returned path should be simplified or not, via douglas-peucker
     * or similar algorithm.
     */
    private GraphHopper setSimplifyResponse(boolean doSimplify) {
        this.simplifyResponse = doSimplify;
        return this;
    }

    /**
     * Configures the underlying storage and response to be used on a well equipped server. Result
     * also optimized for usage in the web module i.e. try reduce network IO.
     */
    public GraphHopper forServer() {
        setSimplifyResponse(true);
        return setInMemory();
    }

    /**
     * Configures the underlying storage to be used on a Desktop computer or within another Java
     * application with enough RAM but no network latency.
     */
    public GraphHopper forDesktop() {
        setSimplifyResponse(false);
        return setInMemory();
    }

    /**
     * Configures the underlying storage to be used on a less powerful machine like Android or
     * Raspberry Pi with only few MB of RAM.
     */
    public GraphHopper forMobile() {
        setSimplifyResponse(false);
        return setMemoryMapped();
    }

    @Override
    public GHResponse route(GHRequest request) {
        GHResponse response = new GHResponse();
        calcPaths((GHRequestIndoor)request, response);
        return response;
    }


}


