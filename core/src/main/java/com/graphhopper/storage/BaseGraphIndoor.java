package com.graphhopper.storage;


import com.graphhopper.routing.util.*;
import com.graphhopper.util.EdgeExplorer;



public class BaseGraphIndoor extends BaseGraph {
    final EncodingManager encodingManager;
    private IndoorExtension indoorExtension;


    public BaseGraphIndoor(Directory dir, final EncodingManager encodingManager, boolean withElevation,
                           InternalGraphEventListener listener, GraphExtension extendedStorage) {
        super(dir, encodingManager, withElevation, listener, extendedStorage);
        this.encodingManager = encodingManager;
        this.indoorExtension = (IndoorExtension) extendedStorage;
    }


    private String getLevel(int baseNode,int adjNode){
        int baseLevel = indoorExtension.getLevel(baseNode);
        int adjLevel = indoorExtension.getLevel(adjNode);
        if(baseLevel == adjLevel)
            return Integer.toString(baseLevel);
        return Integer.toString(baseLevel)+";"+Integer.toString(adjLevel);
    }


    protected static class AllEdgeIteratorIndoor extends AllEdgeIterator implements EdgeIteratorIndoor{
        private BaseGraphIndoor baseGraph;

        public AllEdgeIteratorIndoor(BaseGraphIndoor baseGraph) {
            super(baseGraph);
            this.baseGraph = baseGraph;
        }

        public String getLevel(){
            return baseGraph.getLevel(baseNode,adjNode);
        }

        @Override
        public EdgeIteratorIndoor setLevel(String floor) {
            baseGraph.getExtension().setLevel(baseNode,floor);
            baseGraph.getExtension().setLevel(adjNode,floor);
            return this;
        }


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
        public String getLevel() {
            return baseGraph.getLevel(baseNode,adjNode);
        }


        @Override
        public EdgeIteratorIndoor setLevel(String level) {
            baseGraph.getExtension().setLevel(baseNode,level);
            baseGraph.getExtension().setLevel(adjNode,level);
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



    @Override
    public IndoorExtension getExtension() {
        return indoorExtension;
    }
}
