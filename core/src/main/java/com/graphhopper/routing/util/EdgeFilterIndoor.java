package com.graphhopper.routing.util;

import com.graphhopper.util.EdgeIteratorState;

import java.util.HashSet;
import java.util.Set;

public class EdgeFilterIndoor implements EdgeFilter {
    private String currentLevel = "";
    private Set<String> allLevels = new HashSet<String>();

    public EdgeFilterIndoor(String currentLevel, Set<String> allLevels) {
        this.allLevels = allLevels;
        this.currentLevel = currentLevel;
    }

    @Override
    public boolean accept(EdgeIteratorState edgeState) {
        if (edgeState.getLevel().equals(currentLevel))
            return true;
        if (edgeState.getLevel().contains(";")) {
            String[] levels = edgeState.getLevel().split(";");
            for (String level : levels) {
                if (level.equals(currentLevel))
                    return true;
            }
        }
        return false;
    }

    public void setCurrentLevel(String currentLevel) {
        if(!allLevels.contains(currentLevel)){
            throw new IllegalStateException("level must be part of all the levels in the building. Did you initialize allLevels " +
                    "correctly`?");
        }
        this.currentLevel = currentLevel;
    }

    public void setAllLevels(Set<String> allLevels) {
        this.allLevels = allLevels;
        if (allLevels.isEmpty()) {
            throw new IllegalStateException("You need to specify the levels in the building. If you don't want to provide indoor" +
                    " navigation, use another EdgeFilter!");
        }
    }
}
