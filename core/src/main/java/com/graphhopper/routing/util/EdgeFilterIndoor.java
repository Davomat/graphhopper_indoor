package com.graphhopper.routing.util;

import com.graphhopper.util.EdgeIteratorState;

public class EdgeFilterIndoor implements EdgeFilter {
    private String currentLevel = "";

    public EdgeFilterIndoor(int currentLevel) {
        this.currentLevel = Integer.toString(currentLevel);
    }

    @Override
    public boolean accept(EdgeIteratorState edgeState) {
        EdgeIteratorIndoor edgeIndoor;
        if(edgeState instanceof  EdgeIteratorIndoor)
            edgeIndoor = (EdgeIteratorIndoor) edgeState;
        else{
            throw new IllegalStateException("You need to use an indoor edge for this edge Filter." +
                    "You used " + edgeState.getClass() + " instead");
        }

        if (edgeIndoor.getLevel().equals(currentLevel))
            return true;

        return false;
    }
}
