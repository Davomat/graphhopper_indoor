package com.graphhopper;
import com.graphhopper.routing.util.HintsMap;
import com.graphhopper.util.Helper;
import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.util.shapes.GHPointIndoor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class GHRequestIndoor extends GHRequest {
    private final List<GHPoint> points;
    private final List<Double> favoredHeadings;
    private List<String> pointHints = new ArrayList<>();
    private boolean possibleToAdd = false;

    public GHRequestIndoor() {
        this(5);
    }


    /**
     * Set routing request from specified startPlace (fromLat, fromLon) to endPlace (toLat, toLon)
     */
    public GHRequestIndoor(double fromLat, double fromLon, String fromLevel,double toLat, double toLon,String toLevel) {
        this(new GHPointIndoor(fromLat, fromLon, fromLevel), new GHPointIndoor(toLat, toLon, toLevel));
    }



    @Override
    public List<GHPoint> getPoints() {
        return points;
    }

    public GHRequestIndoor(int size) {
        points = new ArrayList<GHPoint>(size);
        favoredHeadings = new ArrayList<Double>(size);
        possibleToAdd = true;
    }

    /**
     * Set routing request from specified startPlace (fromLat, fromLon) to endPlace (toLat, toLon)
     * with a preferred start and end heading. Headings are north based azimuth (clockwise) in (0,
     * 360) or NaN for equal preference.
     */
    public GHRequestIndoor(double fromLat, double fromLon, double toLat, double toLon,
                     String fromLevel, String toLevel, double startHeading, double endHeading) {
        this(new GHPointIndoor(fromLat, fromLon,fromLevel), new GHPointIndoor(toLat, toLon,toLevel), startHeading, endHeading);
    }



    /**
     * Set routing request from specified startPlace to endPlace with a preferred start and end
     * heading. Headings are north based azimuth (clockwise) in (0, 360) or NaN for equal preference
     */
    public GHRequestIndoor(GHPointIndoor startPlace, GHPointIndoor endPlace, double startHeading, double endHeading) {
        if (startPlace == null)
            throw new IllegalStateException("'from' cannot be null");

        if (endPlace == null)
            throw new IllegalStateException("'to' cannot be null");

        points = new ArrayList<>(2);
        points.add(startPlace);
        points.add(endPlace);

        favoredHeadings = new ArrayList<Double>(2);
        validateAzimuthValue(startHeading);
        favoredHeadings.add(startHeading);
        validateAzimuthValue(endHeading);
        favoredHeadings.add(endHeading);
    }

    public GHRequestIndoor(GHPointIndoor startPlace, GHPointIndoor endPlace) {
        this(startPlace, endPlace, Double.NaN, Double.NaN);
    }

    /**
     * Set routing request
     * <p>
     *
     * @param points          List of stopover points in order: start, 1st stop, 2nd stop, ..., end
     * @param favoredHeadings List of favored headings for starting (start point) and arrival (via
     *                        and end points) Headings are north based azimuth (clockwise) in (0, 360) or NaN for equal
     */
    public GHRequestIndoor(List<GHPoint> points, List<Double> favoredHeadings) {
        if (points.size() != favoredHeadings.size())
            throw new IllegalArgumentException("Size of headings (" + favoredHeadings.size()
                    + ") must match size of points (" + points.size() + ")");

        for (Double heading : favoredHeadings) {
            validateAzimuthValue(heading);
        }
        this.points = points;
        this.favoredHeadings = favoredHeadings;
    }

    /**
     * Set routing request
     * <p>
     *
     * @param points List of stopover points in order: start, 1st stop, 2nd stop, ..., end
     */
    public GHRequestIndoor(List<GHPoint> points) {
        this(points, Collections.nCopies(points.size(), Double.NaN));
    }

    /**
     * Add stopover point to routing request.
     * <p>
     *
     * @param point          geographical position (see GHPoint)
     * @param favoredHeading north based azimuth (clockwise) in (0, 360) or NaN for equal preference
     */
    public GHRequestIndoor addPoint(GHPointIndoor point, double favoredHeading) {
        if (point == null)
            throw new IllegalArgumentException("point cannot be null");

        if (!possibleToAdd)
            throw new IllegalStateException("Please call empty constructor if you intent to use "
                    + "more than two places via addPoint method.");

        points.add(point);
        validateAzimuthValue(favoredHeading);
        favoredHeadings.add(favoredHeading);
        return this;
    }

    /**
     * Add stopover point to routing request.
     * <p>
     *
     * @param point geographical position (see GHPoint)
     */
    public GHRequestIndoor addPoint(GHPointIndoor point) {
        addPoint(point, Double.NaN);
        return this;
    }

    /**
     * @return north based azimuth (clockwise) in (0, 360) or NaN for equal preference
     */
    public double getFavoredHeading(int i) {
        return favoredHeadings.get(i);
    }

    /**
     * @return if there exist a preferred heading for start/via/end point i
     */
    public boolean hasFavoredHeading(int i) {
        if (i >= favoredHeadings.size())
            return false;

        return !Double.isNaN(favoredHeadings.get(i));
    }

    private void validateAzimuthValue(double heading) {
        // heading must be in (0, 360) oder NaN
        if (!Double.isNaN(heading) && (Double.compare(heading, 360) > 0 || Double.compare(heading, 0) < 0))
            throw new IllegalArgumentException("Heading " + heading + " must be in range (0,360) or NaN");
    }



    public GHRequest setLocale(String localeStr) {
        return setLocale(Helper.getLocale(localeStr));
    }



    public GHRequestIndoor setPointHints(List<String> pointHints) {
        this.pointHints = pointHints;
        return this;
    }

    public List<String> getPointHints() {
        return pointHints;
    }



    public String toString() {
        String res = "";
        for (GHPoint point : points) {
            if (res.isEmpty()) {
                res = point.toString();
            } else {
                res += "; " + point.toString();
            }
        }
        if (!super.getAlgorithm().isEmpty())
            res += " (" + super.getAlgorithm() + ")";

        if (!super.getPathDetails().isEmpty())
            res += " (PathDetails: " + super.getPathDetails() + ")";

        if (!pointHints.isEmpty())
            res += " (Hints:" + pointHints + ")";

        return res;
    }

}

