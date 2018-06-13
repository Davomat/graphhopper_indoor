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
import com.graphhopper.storage.BaseGraphIndoor;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;

import java.util.*;

import static com.graphhopper.routing.util.PriorityCode.*;


public class IndoorFlagEncoder extends AbstractFlagEncoder {
    static final int SLOW_SPEED = 2;
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
        restrictions.addAll(Arrays.asList("foot", "access"));
        restrictedValues.add("private");
        restrictedValues.add("no");
        restrictedValues.add("restricted");
        restrictedValues.add("military");
        restrictedValues.add("emergency");

        intendedValues.add("yes");
        intendedValues.add("designated");
        intendedValues.add("official");
        intendedValues.add("permissive");


        setBlockByDefault(false);
        potentialBarriers.add("gate");
        potentialBarriers.add("stairs");

        safeHighwayTags.add("footway");
        safeHighwayTags.add("stairs");
        safeHighwayTags.add("elevator");


        //
        allowedHighwayTags.addAll(safeHighwayTags);
        allowedHighwayTags.addAll(avoidHighwayTags);


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

        priorityWayEncoder = new EncodedValue("PreferWay", shift, 3, 1, 0, 7);
        shift += priorityWayEncoder.getBits();
        return shift;
    }

    @Override
    public int defineRelationBits(int index, int shift) {
        relationCodeEncoder = new EncodedValue("RelationCode", shift, 3, 1, 0, 7);
        return shift + relationCodeEncoder.getBits();
    }

    /**
     * Foot flag encoder does not provide any turn cost / restrictions
     */
    @Override
    public int defineTurnBits(int index, int shift) {
        return shift;
    }

    /**
     * Foot flag encoder does not provide any turn cost / restrictions
     * <p>
     *
     * @return <code>false</code>
     */
    @Override
    public boolean isTurnRestricted(long flag) {
        return false;
    }

    /**
     * Foot flag encoder does not provide any turn cost / restrictions
     * <p>
     *
     * @return 0
     */
    @Override
    public double getTurnCost(long flag) {
        return 0;
    }

    @Override
    public long getTurnFlags(boolean restricted, double costs) {
        return 0;
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

        int priorityFromRelation = 0;
        if (relationFlags != 0)
            priorityFromRelation = (int) relationCodeEncoder.getValue(relationFlags);

        flags = priorityWayEncoder.setValue(flags, handlePriority(way, priorityFromRelation));
        return flags;
    }

    @Override
    public double getDouble(long flags, int key) {
        switch (key) {
            case PriorityWeighting.KEY:
                return (double) priorityWayEncoder.getValue(flags) / BEST.getValue();
            default:
                return super.getDouble(flags, key);
        }
    }

    protected int handlePriority(ReaderWay way, int priorityFromRelation) {
        TreeMap<Double, Integer> weightToPrioMap = new TreeMap<Double, Integer>();
        if (priorityFromRelation == 0)
            weightToPrioMap.put(0d, UNCHANGED.getValue());
        else
            weightToPrioMap.put(110d, priorityFromRelation);

        collect(way, weightToPrioMap);

        // pick priority with biggest order value
        return weightToPrioMap.lastEntry().getValue();
    }

    /**
     * @param weightToPrioMap associate a weight with every priority. This sorted map allows
     *                        subclasses to 'insert' more important priorities as well as overwrite determined priorities.
     */
    void collect(ReaderWay way, TreeMap<Double, Integer> weightToPrioMap) {
        String highway = way.getTag("highway");
        if (way.hasTag("foot", "designated"))
            weightToPrioMap.put(100d, PREFER.getValue());

        double maxSpeed = getMaxSpeed(way);
        if (safeHighwayTags.contains(highway) || maxSpeed > 0 && maxSpeed <= 20) {
            weightToPrioMap.put(40d, PREFER.getValue());
        }
    }

    @Override
    public boolean supports(Class<?> feature) {
        if (super.supports(feature))
            return true;

        return PriorityWeighting.class.isAssignableFrom(feature);
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




