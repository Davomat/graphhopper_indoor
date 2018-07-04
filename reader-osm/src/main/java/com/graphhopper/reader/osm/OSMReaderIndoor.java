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

import com.carrotsearch.hppc.*;
import com.graphhopper.reader.*;
import com.graphhopper.routing.util.AllEdgesIterator;
import com.graphhopper.routing.util.EdgeIteratorIndoor;
import com.graphhopper.storage.*;
import com.graphhopper.util.*;
import com.graphhopper.util.shapes.GHPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.*;
import java.util.List;

import static com.graphhopper.util.Helper.nf;

/**
 * This class provides the functionality to process OSM data with indoor information
 *
 * @author Bettina Auschra
 */
public class OSMReaderIndoor extends OSMReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(OSMReader.class);
    // levels in the building
    private Set<String> allLevels = new HashSet<String>();
    private Date osmDataDate;
    private GraphHopperStorage ghStorage;
    private IndoorExtension indoorExtension;
    private NodeAccess nodeAccess;
    private final DistanceCalc distCalc = Helper.DIST_EARTH;



    public OSMReaderIndoor(GraphHopperStorage ghStorage) {
        super(ghStorage);
        this.ghStorage = ghStorage;
        indoorExtension = (IndoorExtension)ghStorage.getExtension();
        this.nodeAccess = ghStorage.getNodeAccess();
    }



    /**
     * Preprocessing of OSM file to select nodes which are used for highways. This allows a more
     * compact graph data structure.
     */
    @Override
    void preProcess(File osmFile) {
        try (OSMInput in = openOsmInputFile(osmFile)) {
            long tmpWayCounter = 1;
            ReaderElement item;
            while ((item = in.getNext()) != null) {
                if (item.isType(ReaderElement.WAY)) {
                    final ReaderWay way = (ReaderWay) item;
                    boolean valid = filterWay(way);
                    if (valid) {
                        LongIndexedContainer wayNodes = way.getNodes();
                        String level = way.getTag("level","");
                        if (!level.isEmpty()) {
                            if (!level.contains(";")) {
                                allLevels.add(level);
                                int s = wayNodes.size();
                                for (int index = 0; index < s; index++) {
                                    prepareHighwayNode(wayNodes.get(index));
                                }
                            }

                        }


                        if (++tmpWayCounter % 10_000_000 == 0) {
                            this.LOGGER.info(nf(tmpWayCounter) + " (preprocess), osmIdMap:" + nf(getNodeMap().getSize()) + " ("
                                    + getNodeMap().getMemoryUsage() + "MB) " + Helper.getMemInfo());
                        }


                    }

                } else if (item.isType(ReaderElement.FILEHEADER)) {
                    final OSMFileHeader fileHeader = (OSMFileHeader) item;
                    osmDataDate = Helper.createFormatter().parse(fileHeader.getTag("timestamp"));
                }

            }
            if (allLevels.size() > 0) {
                this.ghStorage.getProperties().put("levels", allLevels);

            }
        } catch (Exception ex) {
            throw new RuntimeException("Problem while parsing file", ex);
        }
    }

    @Override
    public Date getDataDate() {
        return osmDataDate;
    }

}
