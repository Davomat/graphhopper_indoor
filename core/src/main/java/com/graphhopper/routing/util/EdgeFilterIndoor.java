package com.graphhopper.routing.util;

import com.graphhopper.util.EdgeIteratorState;

import java.util.Set;

public class EdgeFilterIndoor implements EdgeFilter {
    private String currentFloor = "";

    public EdgeFilterIndoor(String currentFloor) {
        this.currentFloor = currentFloor;
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

        if (edgeIndoor.getFloor().equals(currentFloor))
            return true;

        return false;
    }
}
