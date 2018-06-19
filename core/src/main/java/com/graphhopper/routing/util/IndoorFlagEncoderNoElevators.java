package com.graphhopper.routing.util;

import com.graphhopper.util.PMap;

public class IndoorFlagEncoderNoElevators extends IndoorFlagEncoder{
    public IndoorFlagEncoderNoElevators() {
        this(4, 1);
    }

    public IndoorFlagEncoderNoElevators(PMap properties) {

        this((int) properties.getLong("speedBits", 4),
                properties.getDouble("speedFactor", 1));
        this.properties = properties;
        this.setBlockFords(properties.getBool("block_fords", true));
    }


    public IndoorFlagEncoderNoElevators(int speedBits, double speedFactor) {
        super(speedBits, speedFactor);
        allowedHighwayTags.remove("elevator");
        init();
    }

    @Override
    public String toString() {
        return "noelevators";
    }
}
