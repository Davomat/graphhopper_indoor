package com.graphhopper.storage;

import com.graphhopper.routing.util.EdgeIteratorIndoor;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.search.LevelIndex;





public class BaseGraphIndoor extends BaseGraph{
    final LevelIndex levelIndex;
    int E_LEVEL;

    public BaseGraphIndoor(Directory dir, final EncodingManager encodingManager, boolean withElevation,
                           InternalGraphEventListener listener, GraphExtension extendedStorage) {
        super(dir, encodingManager, withElevation, listener, extendedStorage);
        this.levelIndex = new LevelIndex(dir);

    }

    static abstract class CommonEdgeIteratorIndoor extends CommonEdgeIterator implements EdgeIteratorIndoor{
        BaseGraphIndoor baseGraph;

        public CommonEdgeIteratorIndoor(long edgePointer, EdgeAccess edgeAccess, BaseGraphIndoor baseGraph){
            super(edgePointer,edgeAccess,baseGraph);
        }

        public EdgeIteratorIndoor setLevel(String name) {
          baseGraph.setLevel(edgePointer, name);
            return this;
        }
    }

    private void setLevel(long edgePointer, String level) {
        int levelIndexRef = (int) levelIndex.put(level);
        if (levelIndexRef < 0)
            throw new IllegalStateException("Too many levels are stored, currently limited to int pointer");

        edges.setInt(edgePointer + E_LEVEL, levelIndexRef);
    }


    protected class AllEdgeIteratorIndoor extends AllEdgeIterator{
        public AllEdgeIteratorIndoor(BaseGraphIndoor baseGraph) {
            super(baseGraph);
        }

    }

    @Override
    void setSegmentSize(int bytes) {
        super.setSegmentSize(bytes);
        levelIndex.setSegmentSize(bytes);

    }

    @Override
    void create(long initSize) {
        super.create(initSize);
        levelIndex.create(initSize);

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

    }

    @Override
    public EdgeIteratorIndoor edge(int nodeA, int nodeB) {
        return (EdgeIteratorIndoor) super.edge(nodeA, nodeB);
    }

    @Override
    public EdgeIteratorIndoor getAllEdges() {
        return (EdgeIteratorIndoor) super.getAllEdges();
    }

    @Override
    public EdgeIteratorIndoor getEdgeIteratorState(int edgeId, int adjNode) {
        return (EdgeIteratorIndoor) super.getEdgeIteratorState(edgeId, adjNode);
    }

    @Override
    public EdgeIteratorIndoor edge(int a, int b, double distance, boolean bothDirection) {
        return (EdgeIteratorIndoor) super.edge(a, b, distance, bothDirection);
    }


    @Override
    public Graph getBaseGraph() {
        return this;
    }
}
