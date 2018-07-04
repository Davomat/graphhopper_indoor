package com.graphhopper.storage;

public class IndoorExtension implements GraphExtension {
    private static final int NO_LEVEL = Integer.MIN_VALUE;
    BaseGraphIndoor baseGraph;

    public NodeAccess getNodeAccess() {
        return nodeAccess;
    }

    NodeAccess nodeAccess;
    @Override
    public boolean isRequireNodeField() {
        return true;
    }

    @Override
    public boolean isRequireEdgeField() {
        return false;
    }

    @Override
    public int getDefaultEdgeFieldValue() {
        throw new UnsupportedOperationException("not supported by this graph extension");
    }

    @Override
    public int getDefaultNodeFieldValue() {
        return NO_LEVEL;
    }

    @Override
    public void setSegmentSize(int bytes) {
    }

    @Override
    public GraphExtension copyTo(GraphExtension clonedStorage) {
        if (!(clonedStorage instanceof IndoorExtension)) {
            throw new IllegalStateException("the extended storage to clone must be the same");
        }
        return clonedStorage;
    }

    @Override
    public void init(Graph graph, Directory dir) {
        if(graph instanceof BaseGraphIndoor){
            this.baseGraph = (BaseGraphIndoor)graph;
            this.nodeAccess = graph.getNodeAccess();
        }
        else throw new IllegalStateException("You need to use an indoor graph for this graph extension!");
    }


    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public long getCapacity() {
        return 0;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean loadExisting() {
        return true;
    }

    @Override
    public void flush() {
    }

    @Override
    public GraphExtension create(long byteCount) {
        return this;
    }

    public void setLevel(int index,String level){
        try{
        nodeAccess.setAdditionalNodeField(index,Integer.parseInt(level));
        }
        catch(NumberFormatException exc){}
    }

    public int getLevel(int index){
        return nodeAccess.getAdditionalNodeField(index);
    }
}
