package com.graphhopper.http;

import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopperAPI;
import com.graphhopper.PathWrapper;
import com.graphhopper.reader.osm.GraphHopperIndoor;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.util.StopWatch;
import com.graphhopper.util.shapes.GHPointIndoor;
import com.graphhopper.util.shapes.GHPoint;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import static com.graphhopper.util.Parameters.DETAILS.PATH_DETAILS;
import static com.graphhopper.util.Parameters.Routing.*;
import static com.graphhopper.util.Parameters.Routing.WAY_POINT_MAX_DISTANCE;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

public class GraphHopperServletIndoor extends GraphHopperServlet {
    @Inject
    private GraphHopperAPI graphHopper;
    @Inject
    private EncodingManager encodingManager;
    @Inject
    private RouteSerializer routeSerializer;
    @Inject
    @Named("hasElevation")
    private boolean hasElevation;



    protected List<GHPoint> getPoints(HttpServletRequest req, String key) {
        String[] pointsAsStr = getParams(req, key);
        final List<GHPoint> infoPoints = new ArrayList<>(pointsAsStr.length);
        for (String str : pointsAsStr) {
            String[] fromStrs = str.split(",");
            if (fromStrs.length == 3) {
                GHPointIndoor point = GHPointIndoor.parse(str);
                if (point != null)
                    infoPoints.add(point);
            }
        }
        return infoPoints;
    }
}
