/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.routing.util;

import com.graphhopper.reader.ReaderRelation;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.weighting.PriorityWeighting;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;

import java.util.*;

import static com.graphhopper.routing.util.PriorityCode.*;


public class IndoorFlagEncoder extends AbstractFlagEncoder {
    static final int MEAN_SPEED = 5;
    final Set<String> safeHighwayTags = new HashSet<String>();
    final Set<String> allowedHighwayTags = new HashSet<String>();
    final Set<String> avoidHighwayTags = new HashSet<String>();

    // convert network tag of hiking routes into a way route code
    private EncodedValue priorityWayEncoder;
    private EncodedValue relationCodeEncoder;


    /**
     * Should be only instantiated via EncodingManager
     */
    public IndoorFlagEncoder() {
        this(4, 1);
    }

    public IndoorFlagEncoder(PMap properties) {
        this((int) properties.getLong("speedBits", 4),
                properties.getDouble("speedFactor", 1));
        this.properties = properties;
        this.setBlockFords(properties.getBool("block_fords", true));
    }


    public IndoorFlagEncoder(int speedBits, double speedFactor) {
        super(speedBits, speedFactor, 0);
        restrictions.addAll(Arrays.asList("indoor", "access"));
        restrictedValues.add("private");
        restrictedValues.add("no");


        intendedValues.add("yes");
        intendedValues.add("public");


        setBlockByDefault(false);

        safeHighwayTags.add("footway");
        safeHighwayTags.add("stairs");
        safeHighwayTags.add("steps");
        safeHighwayTags.add("elevator");

        allowedHighwayTags.addAll(safeHighwayTags);

        maxPossibleSpeed = MEAN_SPEED;

        init();
    }


    @Override
    public int getVersion() {
        return 4;
    }

    @Override
    public int defineWayBits(int index, int shift) {
        // first two bits are reserved for route handling in superclass
        shift = super.defineWayBits(index, shift);
        // larger value required - ferries are faster than pedestrians
        speedEncoder = new EncodedDoubleValue("Speed", shift, speedBits, speedFactor, MEAN_SPEED, maxPossibleSpeed);
        shift += speedEncoder.getBits();
        return shift;
    }


    /**
     * Some ways are okay but not separate for pedestrians.
     * <p>
     */
    @Override
    public long acceptWay(ReaderWay way) {
        String highwayValue = way.getTag("highway");

        // check access restrictions
        if (way.hasTag(restrictions, restrictedValues) && !getConditionalTagInspector().isRestrictedWayConditionallyPermitted(way))
            return 0;

        if (!allowedHighwayTags.contains(highwayValue))
            return 0;

        if (getConditionalTagInspector().isPermittedWayConditionallyRestricted(way))
            return 0;

        return acceptBit;
    }

    @Override
    // indoor ways aren't stored as relations
    public long handleRelationTags(ReaderRelation relation, long oldRelationFlags) {
        return 0;
    }

    @Override
    public long handleWayTags(ReaderWay way, long allowed, long relationFlags) {
        if (!isAccept(allowed))
            return 0;

        long flags = 0;

        flags = speedEncoder.setDoubleValue(flags, MEAN_SPEED);
        flags |= directionBitMask;

        return flags;
    }

    @Override
    public String toString() {
        return "indoor";
    }


    @Override
    public void applyWayTags(ReaderWay way, EdgeIteratorState edge) {
        if (edge instanceof EdgeIteratorIndoor) {
            String level = way.getTag("level", "");

            EdgeIteratorIndoor edgeIndoor = (EdgeIteratorIndoor) edge;
            edgeIndoor.setLevel(level);

        }
    }
}




