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
package com.graphhopper.util;

import com.graphhopper.storage.GraphExtension;
import com.graphhopper.storage.IndoorExtension;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.util.shapes.GHPointIndoor;
import com.graphhopper.routing.Path;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

import java.util.*;

/**
 * Slim list to store several points (without the need for a point object). Be aware that the PointList is closely
 * coupled with the {@link ShallowImmutablePointList} if you change it or extend it, you should make sure that your
 * changes play well with the ShallowImmutablePointList to avoid unexpected issues.
 * <p>
 *
 * @author Peter Karich
 */
public class PointListIndoor extends PointList{
    private final static DistanceCalc3D distCalc3D = Helper.DIST_3D;
    final static String ERR_MSG = "Tried to access PointList with too big index!";
    protected int size = 0;
    private double[] latitudes;
    private double[] longitudes;
    private double[] elevations;
    private int[] levels;

    public PointListIndoor() {
        this(10, false);
    }

    public PointListIndoor(int cap, boolean is3D) {
        super(cap,is3D);
        levels = new int[cap];
        latitudes = new double[cap];
        longitudes = new double[cap];
    }



    public PointListIndoor (PointList from, int[] levels){
        this(from.getSize(),from.is3D());
        if(from.getSize()!= levels.length)
            throw new IllegalStateException("Point list and level array must have the same size.\n Size of point list: "+from.getSize()+"\nSize of level array: "+levels.length);
        size = from.getSize();
        super.size = size;
        super.is3D = is3D();
        is3D = from.is3D;
        for(int i = 0;i<size;i++){
            set(i,from.getLat(i),from.getLon(i),from.getEle(i),levels[i]);
        }
    }

    static public PointListIndoor fromPath(Path path, IndoorExtension indoorExtension,int fromLevel,int toLevel){
        PointList list = path.calcPoints();
        if(list.isEmpty())
            return new PointListIndoor();
        List<EdgeIteratorState> edges = path.calcEdges();
        int levels[] = new int[list.getSize()];
        int index = 0;
        int nodeCount = edges.get(0).fetchWayGeometry(1).getSize(); //get the number nodes of the first edge exclusive the adjacent node
        for(int i=0;i<nodeCount;i++){
            levels[index] = fromLevel;
            index++;
        }
        levels[0] = fromLevel;

        for(int i=1;i<edges.size();i++){
            int baseNode= edges.get(i).getBaseNode();
            int level = indoorExtension.getLevel(baseNode);
            nodeCount = edges.get(i).fetchWayGeometry(1).getSize();
            for(int j=0;j<nodeCount;j++){
                levels[index] = level;
                index++;
            }
        }

        levels[levels.length-1] = toLevel;

        PointListIndoor pointListIndoor = new PointListIndoor(list,levels);
        return pointListIndoor;
    }

    @Override
    public int getDimension() {
        if (is3D)
            return 4;
        return 3;
    }


    @Override
    public void setNode(int nodeId, double lat, double lon) {
        set(nodeId, lat, lon, Double.NaN);
    }

    @Override
    public void setNode(int nodeId, double lat, double lon, double ele) {
        set(nodeId, lat, lon, ele);
    }

    public void set(int index, double lat, double lon, double ele) {
        throw new IllegalStateException("You need to specify a level for the node.");    }

    public void set(int index, double lat, double lon, double ele,int level) {
        super.set(index,lat,lon,ele);
        levels[index] = level;
    }

    private void incCap(int newSize) {
        if (newSize <= latitudes.length)
            return;

        int cap = newSize * 2;
        if (cap < 15)
            cap = 15;
        latitudes = Arrays.copyOf(latitudes, cap);
        longitudes = Arrays.copyOf(longitudes, cap);
        levels = Arrays.copyOf(levels,cap);
        if (is3D)
            elevations = Arrays.copyOf(elevations, cap);
    }



    public void add(double lat, double lon, double ele) {
        throw new IllegalStateException("You need to specify a level for the node!");
    }

    public void add(double lat, double lon, int level){
        add(lat,lon,Double.NaN,level);
    }

    public void add(double lat, double lon, double ele, int level) {
        super.add(lat,lon,ele);
        incCap(size + 1);
        levels[size] = level;
        size++;
    }


    public void add(PointAccess pointAccess, int index) {
        if (pointAccess instanceof NodeAccess){
            NodeAccess nodeAccess = (NodeAccess) pointAccess;
            add(nodeAccess.getLatitude(index),nodeAccess.getLon(index),nodeAccess.getElevation(index),nodeAccess.getAdditionalNodeField(index));
        }
        else if(pointAccess instanceof GHPointIndoor) {
            GHPointIndoor indoorPoint = (GHPointIndoor) pointAccess;
            add(indoorPoint.lat, indoorPoint.lon, indoorPoint.ele, Integer.parseInt(indoorPoint.level));
        }
        else throw new UnsupportedOperationException();
    }

    public void add(GHPoint point) {
        if (point instanceof GHPointIndoor){
            GHPointIndoor indoorPoint = (GHPointIndoor)point;
            if(is3D)
                throw new UnsupportedOperationException("Elevation is not supported for indoor points!");
            add(indoorPoint.lat,indoorPoint.lon,Double.NaN,Integer.parseInt(indoorPoint.level));
        }
    }

    public void add(PointList points) {
        if(points instanceof PointListIndoor) {
            super.add(points);
            int newSize = size + points.getSize();
            incCap(newSize);
            for (int i = 0; i < points.getSize(); i++) {
                int tmp = size + i;
                levels[tmp] = ((PointListIndoor)points).getLevel(i);
            }
            size = newSize;
        }else {
            throw new UnsupportedOperationException("You can only add a list of indoor points!");
        }
    }

    public void removeLastPoint() {
        super.removeLastPoint();
        size--;
    }


    public void reverse() {
        super.reverse();
        // in-place reverse
        int max = size / 2;
        for (int i = 0; i < max; i++) {
            int swapIndex = size - i - 1;
            int tmp = levels[i];
            levels[i] = levels[swapIndex];
            levels[swapIndex] = tmp;
            }
        }


    public void clear() {
        super.clear();
        size = 0;
    }

    public void trimToSize(int newSize) {
        super.trimToSize(newSize);
        size = newSize;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < getSize(); i++) {
            if (i > 0)
                sb.append(", ");

            sb.append('(');
            sb.append(getLatitude(i));
            sb.append(',');
            sb.append(getLongitude(i));
            if (this.is3D()) {
                sb.append(',');
                sb.append(getElevation(i));
            }
            sb.append(getLevel(i));
            sb.append(')');
        }
        return sb.toString();
    }



    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        PointListIndoor other = (PointListIndoor) obj;
        if (this.isEmpty() && other.isEmpty())
            return true;

        if (this.getSize() != other.getSize() || this.is3D() != other.is3D())
            return false;

        for (int i = 0; i < size(); i++) {
            if (!NumHelper.equalsEps(getLatitude(i), other.getLatitude(i)))
                return false;

            if (!NumHelper.equalsEps(getLongitude(i), other.getLongitude(i)))
                return false;

            if (this.is3D() && !NumHelper.equalsEps(getElevation(i), other.getElevation(i)))
                return false;
            if(getLevel(i) != other.getLevel(i))
                return false;
        }
        return true;
    }

    /**
     * Clones this PointList. If this PointList was immutable, the cloned will be mutable. If this PointList was a
     * ShallowImmutablePointList, the cloned PointList will be a regular PointList.
     */
    public PointList clone(boolean reverse) {
        PointListIndoor clonePL = new PointListIndoor(getSize(), is3D());
        if (is3D())
            for (int i = 0; i < getSize(); i++) {
                clonePL.add(getLatitude(i), getLongitude(i), getElevation(i),getLevel(i));
            }
        else
            for (int i = 0; i < getSize(); i++) {
                clonePL.add(getLatitude(i), getLongitude(i),getLevel(i));
            }
        if (reverse)
            clonePL.reverse();
        return clonePL;
    }

    /**
     * This method does a deep copy of this object for the specified range.
     *
     * @param from the copying of the old PointList starts at this index
     * @param end  the copying of the old PointList ends at the index before (i.e. end is exclusive)
     */
    public PointList copy(int from, int end) {
        if (from > end)
            throw new IllegalArgumentException("from must be smaller or equal to end");
        if (from < 0 || end > getSize())
            throw new IllegalArgumentException("Illegal interval: " + from + ", " + end + ", size:" + getSize());

        PointListIndoor copyPL = new PointListIndoor(end - from, is3D());
        if (is3D())
            for (int i = from; i < end; i++) {
                copyPL.add(getLatitude(i), getLongitude(i), getElevation(i),getLevel(i));
            }
        else
            for (int i = from; i < end; i++) {
                copyPL.add(getLatitude(i), getLongitude(i), getLevel(i));
            }

        return copyPL;
    }

    public int getLevel(int index){
        return levels[index];
    }

    public int[] getLevels() {
        return levels;
    }


//
//    public static PointList from(LineString lineString) {
//        final PointList pointList = new PointList();
//        for (Coordinate coordinate : lineString.getCoordinates()) {
//            pointList.add(new GHPoint(coordinate.y, coordinate.x));
//        }
//        return pointList;
//    }


}
