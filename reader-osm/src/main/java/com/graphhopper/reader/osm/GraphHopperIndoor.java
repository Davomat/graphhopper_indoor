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
package com.graphhopper.reader.osm;

import com.graphhopper.GraphHopper;
import com.graphhopper.GraphHopperAPI;
import com.graphhopper.reader.DataReader;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.EncodingManagerIndoor;
import com.graphhopper.storage.*;
import com.graphhopper.util.CmdArgs;
import com.graphhopper.util.Unzipper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import static com.graphhopper.util.Helper.*;


/**
 * Easy to use access point to configure import and (offline) routing.
 *
 * @author Peter Karich
 * @see GraphHopperAPI
 *
 * Adapted to Indoor Navigation
 */
public class GraphHopperIndoor extends GraphHopper {

    //for indoor navigation navigation
    private Set<String> allLevels = new HashSet<String>();
    private GraphHopperStorageIndoor ghStorage;
    private EncodingManagerIndoor encodingManager;
    private boolean fullyLoaded = false;


    public GraphHopperIndoor() {
        super();
    }


    protected DataReader createReader(GraphHopperStorage ghStorage) {
        return initDataReader(new OSMReaderIndoor(ghStorage));
    }


    @Override
    public GraphHopperIndoor init(CmdArgs args) {
        super.init(args);
        args = CmdArgs.readFromConfigAndMerge(args, "config", "graphhopper.config");
        int bytesForFlags = args.getInt("graph.bytes_for_flags", 4);
        String flagEncodersStr = args.get("graph.flag_encoders", "");
        if (!flagEncodersStr.isEmpty())
            this.setEncodingManager(new EncodingManagerIndoor(super.getFlagEncoderFactory(), flagEncodersStr, bytesForFlags));
        return this;
    }


    @Override
    public boolean load(String graphHopperFolder) {
        if(!super.load(graphHopperFolder))
            return false;
        this.ghStorage = toIndoor(super.getGraphHopperStorage(),this.encodingManager);
        return true;
    }

    public GraphHopperIndoor setEncodingManager(EncodingManagerIndoor em) {
        ensureNotLoaded();
        this.encodingManager = em;
        return this;
    }

    @Override
    protected void ensureNotLoaded() {
        if (fullyLoaded)
            throw new IllegalStateException("No configuration changes are possible after loading the graph");

    }

    public static GraphHopperStorageIndoor toIndoor(GraphHopperStorage graphHopperStorage, EncodingManagerIndoor encodingManager){
        return new GraphHopperStorageIndoor(graphHopperStorage.getDirectory(),encodingManager,graphHopperStorage.getExtension());
    }




}


