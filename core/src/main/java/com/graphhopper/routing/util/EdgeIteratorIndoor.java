package com.graphhopper.routing.util;

import com.graphhopper.util.EdgeIteratorState;

public interface EdgeIteratorIndoor extends EdgeIteratorState {
    EdgeIteratorIndoor setFloor(String floor);
    String getFloor();
}
