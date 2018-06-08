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

import com.carrotsearch.hppc.*;
import com.graphhopper.coll.*;
import com.graphhopper.coll.LongIntMap;
import com.graphhopper.reader.*;
import com.graphhopper.reader.dem.ElevationProvider;
import com.graphhopper.reader.dem.GraphElevationSmoothing;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.*;
import com.graphhopper.util.*;
import com.graphhopper.util.shapes.GHPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.graphhopper.util.Helper.nf;

/**
 * This class parses an OSM xml or pbf file and creates a graph from it. It does so in a two phase
 * parsing processes in order to reduce memory usage compared to a single parsing processing.
 * <p>
 * 1. a) Reads ways from OSM file and stores all associated node ids in {@link #osmNodeIdToInternalNodeMap}. If a
 * node occurs once it is a pillar node and if more it is a tower node, otherwise
 * {@link #osmNodeIdToInternalNodeMap} returns EMPTY.
 * <p>
 * 1. b) Reads relations from OSM file. In case that the relation is a route relation, it stores
 * specific relation attributes required for routing into {@link #osmWayIdToRouteWeightMap} for all the ways
 * of the relation.
 * <p>
 * 2.a) Reads nodes from OSM file and stores lat+lon information either into the intermediate
 * data structure for the pillar nodes (pillarLats/pillarLons) or, if a tower node, directly into the
 * graphStorage via setLatitude/setLongitude. It can also happen that a pillar node needs to be
 * transformed into a tower node e.g. via barriers or different speed values for one way.
 * <p>
 * 2.b) Reads ways from OSM file and creates edges while calculating the speed etc from the OSM tags.
 * When creating an edge the pillar node information from the intermediate data structure will be
 * stored in the way geometry of that edge.
 * <p>
 *
 * @author Peter Karich
 */
public class OSMReaderIndoor extends OSMReader{
    // levels in the building in the case of indoor navigation
    private Set<String> allLevels = new HashSet<String>();
    private static final Logger LOGGER = LoggerFactory.getLogger(OSMReader.class);
    //private final Map<FlagEncoder, EdgeExplorer> inExplorerMap = new HashMap<FlagEncoder, EdgeExplorer>();
    private Date osmDataDate;



    private GraphHopperStorage ghStorage;



    public OSMReaderIndoor(GraphHopperStorage ghStorage) {
        super(ghStorage);
        this.ghStorage = ghStorage;
        //this.ghStorage = new GraphHopperStorageIndoor(ghStorage.getDirectory(),ghStorage.getEncodingManager(),false,ghStorage.getExtension());
    }


    /**
     * Preprocessing of OSM file to select nodes which are used for highways. This allows a more
     * compact graph data structure.
     */
    @Override
    void preProcess(File osmFile) {
        try (OSMInput in = openOsmInputFile(osmFile)) {
            long tmpWayCounter = 1;
            long tmpRelationCounter = 1;
            ReaderElement item;
            while ((item = in.getNext()) != null) {
                if (item.isType(ReaderElement.WAY)) {
                    final ReaderWay way = (ReaderWay) item;
                    boolean valid = filterWay(way);
                    if (valid) {
                        LongIndexedContainer wayNodes = way.getNodes();
                        int s = wayNodes.size();
                        for (int index = 0; index < s; index++) {
                            prepareHighwayNode(wayNodes.get(index));
                        }

                        if (++tmpWayCounter % 10_000_000 == 0) {
                            this.LOGGER.info(nf(tmpWayCounter) + " (preprocess), osmIdMap:" + nf(getNodeMap().getSize()) + " ("
                                    + getNodeMap().getMemoryUsage() + "MB) " + Helper.getMemInfo());
                        }
                        String level = way.getTag("level");
                        if (level != null) {
                            if (level.contains(";")) {
                                String[] levels = level.split(";");
                                for (String level2 : levels) {
                                    if (!allLevels.contains(level)) {
                                        allLevels.add(level2);
                                    }
                                }

                            }
                                allLevels.add(level);

                        }
                    }
                } else if (item.isType(ReaderElement.RELATION)) {
                    final ReaderRelation relation = (ReaderRelation) item;
                    if (!relation.isMetaRelation() && relation.hasTag("type", "route"))
                        prepareWaysWithRelationInfo(relation);

                    if (++tmpRelationCounter % 100_000 == 0) {
                        LOGGER.info(nf(tmpRelationCounter) + " (preprocess), osmWayMap:" + nf(getRelFlagsMap().size())
                                + " " + Helper.getMemInfo());
                    }
                } else if (item.isType(ReaderElement.FILEHEADER)) {
                    final OSMFileHeader fileHeader = (OSMFileHeader) item;
                    osmDataDate = Helper.createFormatter().parse(fileHeader.getTag("timestamp"));
                }

            }
            if(allLevels.size()>0){
                this.ghStorage.getProperties().put("levels",allLevels);

            }
        } catch (Exception ex) {
            throw new RuntimeException("Problem while parsing file", ex);
        }
    }

    @Override
    public Date getDataDate() {
        return osmDataDate;
    }


    @Override
    void processWay(ReaderWay way) {
        super.processWay(way);
    }




}
