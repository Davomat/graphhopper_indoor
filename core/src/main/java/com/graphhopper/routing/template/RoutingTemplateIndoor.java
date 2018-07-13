package com.graphhopper.routing.template;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;

import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.Path;

import com.graphhopper.routing.QueryGraph;
import com.graphhopper.routing.RoutingAlgorithmFactory;
import com.graphhopper.routing.util.*;
import com.graphhopper.storage.IndoorExtension;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.PointList;
import com.graphhopper.util.PointListIndoor;
import com.graphhopper.util.exceptions.PointNotFoundException;
import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.util.shapes.GHPointIndoor;

import java.util.ArrayList;
import java.util.List;

public class RoutingTemplateIndoor extends ViaRoutingTemplate {

    private final LocationIndex locationIndex;
    private int[] levels;
    private IndoorExtension indoorExtension;

    public RoutingTemplateIndoor(GHRequest ghRequest, GHResponse ghRsp, LocationIndex locationIndex, IndoorExtension indoorExtension) {
        super(ghRequest, ghRsp, locationIndex);
        this.locationIndex = locationIndex;
        this.indoorExtension = indoorExtension;
    }

    @Override
    public List<QueryResult> lookup(List<GHPoint> points, FlagEncoder encoder) {
        if (points.size() < 2)
            throw new IllegalArgumentException("At least 2 points have to be specified, but was:" + points.size());
        queryResults = new ArrayList<>(points.size());
        levels = new int[points.size()];
        for (int placeIndex = 0; placeIndex < points.size(); placeIndex++) {
            GHPointIndoor point = (GHPointIndoor)points.get(placeIndex);
            levels[placeIndex] = ((GHPointIndoor) points.get(placeIndex)).getLevel();
            QueryResult res;
            res = locationIndex.findClosest(point.lat, point.lon, new EdgeFilterIndoor(point.level));

            if (!res.isValid())
                ghResponse.addError(new PointNotFoundException("Cannot find point " + placeIndex + ": " + point, placeIndex));

            queryResults.add(res);
        }

        return queryResults;
    }


    @Override
    protected PointList getWaypoints() {
        PointList pointList= super.getWaypoints();
        PointListIndoor pointListIndoor = new PointListIndoor(pointList,levels);
        return pointListIndoor;

    }

    @Override
    public List<Path> calcPaths(QueryGraph queryGraph, RoutingAlgorithmFactory algoFactory, AlgorithmOptions algoOpts) {
        List<Path> paths = super.calcPaths(queryGraph, algoFactory, algoOpts);
        int levelIndex = 0;
        for(int i=0;i<paths.size();i++){
            Path path = paths.get(i);
            int[] partLevels = {levels[levelIndex],levels[levelIndex+1]};
            path.setLevels(partLevels);
        }
        return paths;
    }
}

