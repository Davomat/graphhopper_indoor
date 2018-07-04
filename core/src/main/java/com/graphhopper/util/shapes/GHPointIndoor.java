package com.graphhopper.util.shapes;

public class GHPointIndoor extends GHPoint3D {
    public double lat;
    public double lon;
    public String level;

    public GHPointIndoor(double lat, double lon, String level) {
        super(lat,lon,Double.NaN);
        this.lat = lat;
        this.lon = lon;
        this.level = level;

    }

    public static GHPointIndoor parse(String str) {
        return parse(str, false);
    }

    private static GHPointIndoor parse(String str, boolean lonLatOrder) {
        String[] fromStrs = str.split(",");
        if (fromStrs.length == 3) {
            try {
                double fromLat = Double.parseDouble(fromStrs[0]);
                double fromLon = Double.parseDouble(fromStrs[1]);
                String level = fromStrs[2];
                if (lonLatOrder)
                    return new GHPointIndoor(fromLon, fromLat,level);

                return new GHPointIndoor(fromLat, fromLon,level);
            } catch (Exception ex) {
            }
        }
        return null;
    }

    public String getLevel(){
        return this.level;
    }

    @Override
    public boolean isValid() {
        return super.isValid() && this.level!=null;
    }

    @Override
    public boolean equals(Object obj) {
        final GHPointIndoor other = (GHPointIndoor) obj;
        return super.equals(obj)&&other.level == this.level;
    }

    @Override
    public String toString() {
        return super.toString()+","+this.level;
    }
}