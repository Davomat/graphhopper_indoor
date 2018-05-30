package com.graphhoper.disabledrouting;

import com.graphhopper.GraphHopper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.util.CmdArgs;

public class AppMain {

    public static void main(String[] strs) throws Exception {
        CmdArgs args = CmdArgs.read(strs);
        GraphHopper hopper = new GraphHopperOSM().init(args).importOrLoad();

    }

}
