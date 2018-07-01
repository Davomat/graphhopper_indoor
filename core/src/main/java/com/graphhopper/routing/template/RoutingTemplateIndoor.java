package com.graphhopper.routing.template;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GHRequestIndoor;
import com.graphhopper.GHResponse;
import com.graphhopper.PathWrapper;
import com.graphhopper.routing.*;
import com.graphhopper.routing.util.*;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.*;
import com.graphhopper.util.exceptions.PointNotFoundException;
import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.util.shapes.GHPointIndoor;

import java.util.ArrayList;
import java.util.List;

public class RoutingTemplateIndoor extends ViaRoutingTemplate implements RoutingTemplate {

    protected final GHRequestIndoor ghRequest;
    protected final GHResponse ghResponse;
    protected final PathWrapper altResponse = new PathWrapper();
    private final LocationIndex locationIndex;
    // result from route
    protected List<Path> pathList;

    public RoutingTemplateIndoor(GHRequest ghRequest, GHResponse ghRsp, LocationIndex locationIndex) {
        super(ghRequest, ghRsp, locationIndex);
        this.ghRequest = (GHRequestIndoor) ghRequest;
        this.ghResponse = ghRsp;
        this.locationIndex = locationIndex;
    }

    @Override
    public List<QueryResult> lookup(List<GHPoint> points, FlagEncoder encoder) {
        if (points.size() < 2)
            throw new IllegalArgumentException("At least 2 points have to be specified, but was:" + points.size());
        queryResults = new ArrayList<>(points.size());
        for (int placeIndex = 0; placeIndex < points.size(); placeIndex++) {
            GHPointIndoor point = (GHPointIndoor)points.get(placeIndex);
            QueryResult res;
            res = locationIndex.findClosest(point.lat, point.lon, new EdgeFilterIndoor(point.level));

            if (!res.isValid())
                ghResponse.addError(new PointNotFoundException("Cannot find point " + placeIndex + ": " + point, placeIndex));

            queryResults.add(res);
        }

        return queryResults;
    }

    @Override
    public List<Path> calcPaths(QueryGraph queryGraph, RoutingAlgorithmFactory algoFactory, AlgorithmOptions algoOpts) {
        long visitedNodesSum = 0L;
        int pointCounts = ghRequest.getPoints().size();
        pathList = new ArrayList<>(pointCounts - 1);
        QueryResult fromQResult = queryResults.get(0);
        StopWatch sw;
        for (int placeIndex = 1; placeIndex < pointCounts; placeIndex++) {
            if (placeIndex == 1) {
                // enforce start direction
                queryGraph.enforceHeading(fromQResult.getClosestNode(), ghRequest.getFavoredHeading(0), false);
            }

            QueryResult toQResult = queryResults.get(placeIndex);

            // enforce end direction
            queryGraph.enforceHeading(toQResult.getClosestNode(), ghRequest.getFavoredHeading(placeIndex), true);

            sw = new StopWatch().start();
            RoutingAlgorithm algo = algoFactory.createAlgo(queryGraph, algoOpts);
            String debug = ", algoInit:" + sw.stop().getSeconds() + "s";

            sw = new StopWatch().start();
            List<Path> tmpPathList = algo.calcPaths(fromQResult.getClosestNode(), toQResult.getClosestNode());
            debug += ", " + algo.getName() + "-routing:" + sw.stop().getSeconds() + "s";
            if (tmpPathList.isEmpty())
                throw new IllegalStateException("At least one path has to be returned for " + fromQResult + " -> " + toQResult);

            int idx = 0;
            for (Path path : tmpPathList) {
                if (path.getTime() < 0)
                    throw new RuntimeException("Time was negative " + path.getTime() + " for index " + idx + ". Please report as bug and include:" + ghRequest);

                pathList.add(path);
                debug += ", " + path.getDebugInfo();
                idx++;
            }

            altResponse.addDebugInfo(debug);

            // reset all direction enforcements in queryGraph to avoid influencing next path
            queryGraph.clearUnfavoredStatus();

            if (algo.getVisitedNodes() >= algoOpts.getMaxVisitedNodes())
                throw new IllegalArgumentException("No path found due to maximum nodes exceeded " + algoOpts.getMaxVisitedNodes());

            visitedNodesSum += algo.getVisitedNodes();
            fromQResult = toQResult;
        }

        ghResponse.getHints().put("visited_nodes.sum", visitedNodesSum);
        ghResponse.getHints().put("visited_nodes.average", (float) visitedNodesSum / (pointCounts - 1));

        return pathList;
    }

    @Override
    public boolean isReady(PathMerger pathMerger, Translation tr) {
        if (ghRequest.getPoints().size() - 1 != pathList.size())
            throw new RuntimeException("There should be exactly one more points than paths. points:" + ghRequest.getPoints().size() + ", paths:" + pathList.size());

        altResponse.setWaypoints(getWaypoints());
        ghResponse.add(altResponse);
        pathMerger.doWork(altResponse, pathList, tr);
        return true;
    }

}

