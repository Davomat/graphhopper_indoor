package com.graphhopper.routing.template;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;

import com.graphhopper.routing.util.*;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.exceptions.PointNotFoundException;
import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.util.shapes.GHPointIndoor;

import java.util.ArrayList;
import java.util.List;

public class RoutingTemplateIndoor extends ViaRoutingTemplate implements RoutingTemplate {

    private final LocationIndex locationIndex;

    public RoutingTemplateIndoor(GHRequest ghRequest, GHResponse ghRsp, LocationIndex locationIndex) {
        super(ghRequest, ghRsp, locationIndex);
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


}

