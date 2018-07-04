package com.graphhopper.storage;


import com.graphhopper.routing.util.*;
import com.graphhopper.search.FloorIndex;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.Helper;
import com.graphhopper.util.PointList;
import com.graphhopper.util.PointListIndoor;

import java.util.HashSet;


public class BaseGraphIndoor extends BaseGraph {
    final FloorIndex floorIndex;
    int E_FLOOR;
    final EncodingManager encodingManager;
    private final InternalGraphEventListener listener;
    private final Directory dir;


    public BaseGraphIndoor(Directory dir, final EncodingManager encodingManager, boolean withElevation,
                           InternalGraphEventListener listener, GraphExtension extendedStorage) {
        super(dir, encodingManager, withElevation, listener, extendedStorage);
        this.floorIndex = new FloorIndex(dir);
        this.encodingManager = encodingManager;
        this.listener = listener;
        this.dir = dir;
    }



    private void setFloor(long edgePointer, String floor) {
        int floorIndexRef = (int) floorIndex.put(floor.trim());
        if (floorIndexRef < 0)
            throw new IllegalStateException("Too many floors are stored, currently limited to int pointer");
        edges.setInt(edgePointer + E_FLOOR, floorIndexRef);
    }


    protected static class AllEdgeIteratorIndoor extends AllEdgeIterator implements EdgeIteratorIndoor{
        private BaseGraphIndoor baseGraph;

        public AllEdgeIteratorIndoor(BaseGraphIndoor baseGraph) {
            super(baseGraph);
            this.baseGraph = baseGraph;
        }

        public String getFloor(){
            int floorIndexRef = baseGraph.edges.getInt(edgePointer + baseGraph.E_FLOOR);
            return baseGraph.floorIndex.get(floorIndexRef);
        }

        @Override
        public EdgeIteratorIndoor setFloor(String floor) {
            baseGraph.setFloor(edgePointer, floor);
            ((IndoorExtension)baseGraph.getExtension()).setLevel(baseNode,floor);
            return this;
        }


    }

    @Override
    void setSegmentSize(int bytes) {
        super.setSegmentSize(bytes);
        floorIndex.setSegmentSize(bytes);

    }

    @Override
    void create(long initSize) {
        super.create(initSize);
        floorIndex.create(initSize);
        initStorage();

    }

    @Override
    void flush() {
        super.flush();
        floorIndex.flush();
    }

    @Override
    void close() {
        super.close();
        floorIndex.close();
    }

    void _copyTo(BaseGraphIndoor clonedG) {
        super._copyTo(clonedG);
        //floors
        floorIndex.copyTo(clonedG.floorIndex);
    }

    @Override
    void initStorage() {
        super.initStorage();
        E_FLOOR = nextEdgeEntryIndex(4);
        initNodeAndEdgeEntrySize();
        listener.initStorage();

    }

    @Override
    void loadExisting(String dim) {
        super.loadExisting(dim);
        if (!floorIndex.loadExisting())
            throw new IllegalStateException("Cannot load name index. corrupt file or directory? " + dir);
        initStorage();
    }

    protected static class EdgeIterableIndoor extends EdgeIterable implements EdgeIteratorIndoor{
        private  BaseGraphIndoor baseGraph;
        EdgeFilter filter;
        public EdgeIterableIndoor(BaseGraphIndoor baseGraph, EdgeAccess edgeAccess, EdgeFilter filter){
            super(baseGraph,edgeAccess,filter);
            this.baseGraph = baseGraph;
            this.filter = filter;
        }

        @Override
        public String getFloor() {
            int floorIndexRef = baseGraph.edges.getInt(edgePointer + baseGraph.E_FLOOR);
            return baseGraph.floorIndex.get(floorIndexRef);
        }


        @Override
        public EdgeIteratorIndoor setFloor(String floor) {
            baseGraph.setFloor(edgePointer, floor);
            ((IndoorExtension)baseGraph.getExtension()).setLevel(baseNode,floor);
            ((IndoorExtension)baseGraph.getExtension()).setLevel(adjNode,floor);
            return this;
        }
    }


    @Override
    public EdgeIteratorIndoor edge(int nodeA, int nodeB) {
        if (isFrozen())
            throw new IllegalStateException("Cannot create edge if graph is already frozen");

        ensureNodeIndex(Math.max(nodeA, nodeB));
        int edgeId = edgeAccess.internalEdgeAdd(nextEdgeId(), nodeA, nodeB);
        EdgeIterableIndoor iter = new EdgeIterableIndoor(this, edgeAccess, EdgeFilter.ALL_EDGES);
        boolean ret = iter.init(edgeId, nodeB);
        assert ret;
        if (extStorage.isRequireEdgeField())
            iter.setAdditionalField(extStorage.getDefaultEdgeFieldValue());

        return iter;
    }

    @Override
    public EdgeExplorer createEdgeExplorer(EdgeFilter filter) {
        return new EdgeIterableIndoor(this,edgeAccess,filter);
    }

    @Override
    public AllEdgesIterator getAllEdges() {
        return new AllEdgeIteratorIndoor(this);
    }

    private void setWayGeometry_(PointList pillarNodes, long edgePointer, boolean reverse) {
//        if (pillarNodes != null && !pillarNodes.isEmpty()) {
//            if (pillarNodes.getDimension() != nodeAccess.getDimension())
//                throw new IllegalArgumentException("Cannot use pointlist which is " + pillarNodes.getDimension()
//                        + "D for graph which is " + nodeAccess.getDimension() + "D");
//
//            long existingGeoRef = Helper.toUnsignedLong(edges.getInt(edgePointer + E_GEO));
//
//            int len = pillarNodes.getSize();
//            int dim = nodeAccess.getDimension();
//            if (existingGeoRef > 0) {
//                final int count = wayGeometry.getInt(existingGeoRef * 4L);
//                if (len <= count) {
//                    setWayGeometryAtGeoRef(pillarNodes, edgePointer, reverse, existingGeoRef);
//                    return;
//                }
//            }
//
//            long nextGeoRef = nextGeoRef(len * dim);
//            setWayGeometryAtGeoRef(pillarNodes, edgePointer, reverse, nextGeoRef);
//        } else {
//            edges.setInt(edgePointer + E_GEO, 0);
//        }
        System.out.println("Na immerhin");
    }




}
