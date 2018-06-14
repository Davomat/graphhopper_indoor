package com.graphhopper.routing.util;

import com.graphhopper.util.EdgeIteratorState;

import java.util.Set;

public class EdgeFilterIndoor implements EdgeFilter {
    private String currentFloor = "";
    private Set<String> allFloors;

    public EdgeFilterIndoor(String currentFloor, Set<String> allFloors) {
        this.allFloors = allFloors;
        this.currentFloor = currentFloor;
        allFloors.add(currentFloor);
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

    public void setCurrentFloor(String currentFloor) {
        if(!allFloors.contains(currentFloor)){
            throw new IllegalStateException("floor must be part of all the floors in the building. Did you initialize allFloors " +
                    "correctly`?");
        }
        this.currentFloor = currentFloor;
    }

    public void setAllFloors(Set<String> allFloors) {
        this.allFloors = allFloors;
        if (allFloors.isEmpty()) {
            throw new IllegalStateException("You need to specify the floors in the building. If you don't want to provide indoor" +
                    " navigation, use another EdgeFilter!");
        }
    }
}
