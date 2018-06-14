package com.graphhopper.storage;


import com.graphhopper.routing.util.EdgeFilterIndoor;
import com.graphhopper.routing.util.EdgeIteratorIndoor;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.search.LevelIndex;
import com.graphhopper.search.NameIndex;
import com.graphhopper.util.BitUtil;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIteratorState;

import java.util.HashSet;
import java.util.Set;


public class BaseGraphIndoor extends BaseGraph {
    final LevelIndex levelIndex;
    int E_LEVEL;
    final EncodingManager encodingManager;
    private final InternalGraphEventListener listener;
    private final Directory dir;



    public BaseGraphIndoor(Directory dir, final EncodingManager encodingManager, boolean withElevation,
                           InternalGraphEventListener listener, GraphExtension extendedStorage) {
        super(dir, encodingManager, withElevation, listener, extendedStorage);
        this.levelIndex = new LevelIndex(dir);
        this.encodingManager = encodingManager;
        this.listener = listener;
        this.dir = dir;
    }

    static abstract class CommonEdgeIteratorIndoor extends CommonEdgeIterator implements EdgeIteratorIndoor{
        BaseGraphIndoor baseGraph;
        protected long edgePointer;
        protected int baseNode;
        protected int adjNode;
        protected EdgeAccess edgeAccess;

        public CommonEdgeIteratorIndoor(long edgePointer, EdgeAccess edgeAccess, BaseGraphIndoor baseGraph){
            super(edgePointer,edgeAccess,baseGraph);
            this.baseGraph = baseGraph;
            this.edgePointer = edgePointer;
            this.edgeAccess = edgeAccess;
        }

        public EdgeIteratorIndoor setLevel(String level) {
            baseGraph.setLevel(edgePointer, level);
            return this;
        }

        public String getLevel() {
            int levelIndexRef = baseGraph.edges.getInt(edgePointer + baseGraph.E_LEVEL);
            return baseGraph.levelIndex.get(levelIndexRef);
        }
    }

    private void setLevel(long edgePointer, String level) {
        int levelIndexRef = (int) levelIndex.put(level);
        if (levelIndexRef < 0)
            throw new IllegalStateException("Too many levels are stored, currently limited to int pointer");

        edges.setInt(edgePointer + E_LEVEL, levelIndexRef);
    }


//    protected class AllEdgeIteratorIndoor extends AllEdgeIterator{
//        public AllEdgeIteratorIndoor(BaseGraphIndoor baseGraph) {
//            super(baseGraph);
//        }
//
//    }

    @Override
    void setSegmentSize(int bytes) {
        super.setSegmentSize(bytes);
        levelIndex.setSegmentSize(bytes);

    }

    @Override
    void create(long initSize) {
        super.create(initSize);
        levelIndex.create(initSize);
        initStorage();

    }

    @Override
    void flush() {
        super.flush();
        levelIndex.flush();
    }

    @Override
    void close() {
        super.close();
        levelIndex.close();
    }

    void _copyTo(BaseGraphIndoor clonedG) {
        super._copyTo(clonedG);
        //levels
        levelIndex.copyTo(clonedG.levelIndex);
    }

    @Override
    void initStorage() {
        super.initStorage();
        E_LEVEL = nextEdgeEntryIndex(4);
        initNodeAndEdgeEntrySize();
        listener.initStorage();

    }

    @Override
    void loadExisting(String dim) {
        super.loadExisting(dim);
        if (!levelIndex.loadExisting())
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
        public String getLevel() {
            int levelIndexRef = baseGraph.edges.getInt(edgePointer + baseGraph.E_LEVEL);
            return baseGraph.levelIndex.get(levelIndexRef);
        }


        @Override
        public EdgeIteratorIndoor setLevel(String level) {
            baseGraph.setLevel(edgePointer, level);
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
    public EdgeExplorer createEdgeExplorer() {
        return this.createEdgeExplorer(new EdgeFilterIndoor("0", new HashSet<String>()));
    }

    @Override
    public EdgeExplorer createEdgeExplorer(EdgeFilter filter) {
        return new EdgeIterableIndoor(this,edgeAccess,filter);
    }


}
