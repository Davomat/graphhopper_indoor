package com.graphhopper.routing.util;

import com.graphhopper.util.EdgeIteratorState;

public interface EdgeIteratorIndoor extends AllEdgesIterator {
   EdgeIteratorIndoor setLevel(String level);
   String getLevel();

}

