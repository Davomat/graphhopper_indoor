package com.graphhopper.routing.util;

import com.graphhopper.util.PMap;

import java.util.Arrays;

public class IndoorFlagEncoderNoStairs extends IndoorFlagEncoder{
    public IndoorFlagEncoderNoStairs() {
        this(4, 1);
    }

    public IndoorFlagEncoderNoStairs(PMap properties) {
        this((int) properties.getLong("speedBits", 4),
                properties.getDouble("speedFactor", 1));
        this.properties = properties;
        this.setBlockFords(properties.getBool("block_fords", true));;
    }


    public IndoorFlagEncoderNoStairs(int speedBits, double speedFactor) {
        super(speedBits, speedFactor);
        allowedHighwayTags.remove("stairs");

        init();
    }

    @Override
    public String toString() {
        return "nostairs";
    }
}
