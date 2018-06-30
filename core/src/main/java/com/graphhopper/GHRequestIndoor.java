package com.graphhopper;
import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.util.shapes.GHPointIndoor;

import java.util.ArrayList;
import java.util.List;

public class GHRequestIndoor extends GHRequest {
    private final List<GHPointIndoor> points;
    private List<String> pointHints = new ArrayList<>();
    private boolean possibleToAdd = false;

    public GHRequestIndoor() {
        this(5);
    }

    public GHRequestIndoor(int size) {
        points = new ArrayList<GHPointIndoor>(size);
        possibleToAdd = true;
    }


    /**
     * Set routing request from specified startPlace (fromLat, fromLon) to endPlace (toLat, toLon)
     */
    public GHRequestIndoor(double fromLat, double fromLon, String fromLevel,double toLat, double toLon,String toLevel) {
        this(new GHPointIndoor(fromLat, fromLon, toLevel), new GHPointIndoor(toLat, toLon, toLevel));
    }


    /**
     * Set routing request
     * <p>
     *
     * @param points   List of stopover points in order: start, 1st stop, 2nd stop, ..., end
     */
    public GHRequestIndoor(List<GHPointIndoor> points) {
        this.points = points;
    }



    /**
     * Set routing request from specified startPlace to endPlace
     */
    public GHRequestIndoor(GHPointIndoor startPlace, GHPointIndoor endPlace) {
        if (startPlace == null)
            throw new IllegalStateException("'from' cannot be null");

        if (endPlace == null)
            throw new IllegalStateException("'to' cannot be null");

        points = new ArrayList<GHPointIndoor>(2);
        points.add(startPlace);
        points.add(endPlace);
    }

    /**
     * Add stopover point to routing request.
     * <p>
     *@param point geographical position (see GHPoint)
     */
    public GHRequestIndoor addPoint(GHPointIndoor point) {
        if (point == null)
            throw new IllegalArgumentException("point cannot be null");

        if (!possibleToAdd)
            throw new IllegalStateException("Please call empty constructor if you intent to use "
                    + "more than two places via addPoint method.");

        points.add(point);
        return this;
    }

    public List<GHPointIndoor> getIndoorPoints() {
        return points;
    }

}
