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
package com.graphhopper.ui;

import com.carrotsearch.hppc.IntIndexedContainer;
import com.graphhopper.coll.GHBitSet;
import com.graphhopper.coll.GHTBitSet;
import com.graphhopper.GraphHopper;

import com.graphhopper.reader.osm.GraphHopperOSM;

import com.graphhopper.routing.*;
import com.graphhopper.routing.ch.PreparationWeighting;
import com.graphhopper.routing.ch.PrepareContractionHierarchies;
import com.graphhopper.routing.util.*;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.*;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.*;
import com.graphhopper.util.Parameters.Algorithms;
import com.graphhopper.util.shapes.BBox;
import com.graphhopper.routing.util.EdgeFilterIndoor;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Random;
import java.util.Arrays;


/**
 * A rough graphical user interface for visualizing the OSM graph. Mainly for debugging algorithms
 * and spatial data structures. See e.g. this blog post:
 * https://graphhopper.com/blog/2016/01/19/alternative-roads-to-rome/
 * <p>
 * Use the web module for a better/faster/userfriendly/... alternative!
 * <p>
 *
 * @author Peter Karich
 *
 * adapted for Indoor Navigation
 */
public class MiniGraphUIIndoor {
    //    private final Graph graph;
    private final Graph routingGraph;
    private final NodeAccess nodeAccess;
    private final MapLayer pathLayer;
    private Weighting weighting;
    private FlagEncoder encoder;
    private RoutingAlgorithmFactory algoFactory;
    private AlgorithmOptions algoOpts;
    // for moving
    int currentPosX;
    int currentPosY;
    String currentLevel = "0";
    String fromLevel = "";
    String toLevel = "";
    private Logger logger = LoggerFactory.getLogger(getClass());
    private Path path;
    private LocationIndexTree index;
    private String latLon = "";
    private GraphicsWrapper mg;
    private JPanel infoPanel;
    private JPanel settings;
    private LayeredPanel mainPanel;
    private WayLayer wayLayer;
    private boolean fastPaint = false;
    private QueryResult fromRes;
    private QueryResult toRes;
    private ButtonGroup allLevelButtons = new ButtonGroup();
    private ButtonGroup allEncoders = new ButtonGroup();
    HintsMap map;
    final Graph graph;
    private IndoorExtension indoorExtension;
    Color edgeOnSameLevel = new Color(234, 0, 0);
    Color edgeOnDifferentLevel = new Color(161, 161, 161);
    Color wayOnSameLevel = new Color(49, 226, 141);
    Color wayOnDifferentLevel = new Color(198, 198, 198);

    public MiniGraphUIIndoor(GraphHopper hopper, boolean debug) {
        graph = hopper.getGraphHopperStorage();
        this.nodeAccess = graph.getNodeAccess();
        this.indoorExtension = (IndoorExtension)graph.getExtension();
        encoder = hopper.getEncodingManager().getEncoder("indoor");
        map = new HintsMap("fastest").
                        setVehicle("indoor");

        boolean ch = false; //currently doesn't work with contraction hierarchies!!
        if (ch) {
            map.put(Parameters.Landmark.DISABLE, true);
            weighting = hopper.getCHFactoryDecorator().getWeightings().get(0);
            routingGraph = hopper.getGraphHopperStorage().getGraph(CHGraph.class, weighting);

            final RoutingAlgorithmFactory tmpFactory = hopper.getAlgorithmFactory(map);
            algoFactory = new RoutingAlgorithmFactory() {

                @Override
                public RoutingAlgorithm createAlgo(Graph g, AlgorithmOptions opts) {
                    // doable but ugly
                    Weighting w = ((PrepareContractionHierarchies) tmpFactory).getWeighting();
                    return new TmpAlgo(g, new PreparationWeighting(w), TraversalMode.NODE_BASED, mg).
                            setEdgeFilter(new LevelEdgeFilter((CHGraph) routingGraph));
                }

                class TmpAlgo extends DijkstraBidirectionCH implements DebugAlgo {
                    private final GraphicsWrapper mg;
                    private Graphics2D g2;

                    public TmpAlgo(Graph graph, Weighting type, TraversalMode tMode, GraphicsWrapper mg) {
                        super(graph, type, tMode);
                        this.mg = mg;
                    }

                    @Override
                    public void setGraphics2D(Graphics2D g2) {
                        this.g2 = g2;
                    }

                    @Override
                    public void updateBestPath(EdgeIteratorState es, SPTEntry bestEE, int currLoc) {
                        if (g2 != null)
                            mg.plotNode(g2, currLoc, Color.YELLOW, 6);

                        super.updateBestPath(es, bestEE, currLoc);
                    }
                }
            };
            algoOpts = new AlgorithmOptions(Algorithms.DIJKSTRA_BI, weighting);

        } else {
            map.put(Parameters.CH.DISABLE, true);
//            map.put(Parameters.Landmark.DISABLE, true);
            routingGraph = graph;
            weighting = hopper.createWeighting(map, encoder, graph);
            final RoutingAlgorithmFactory tmpFactory = hopper.getAlgorithmFactory(map);
            algoFactory = new RoutingAlgorithmFactory() {

                @Override
                public RoutingAlgorithm createAlgo(Graph g, AlgorithmOptions opts) {
                    RoutingAlgorithm algo = tmpFactory.createAlgo(g, opts);
                    if (algo instanceof AStarBidirection) {
                        return new DebugAStarBi(g, opts.getWeighting(), opts.getTraversalMode(), mg).
                                setApproximation(((AStarBidirection) algo).getApproximation());
                    } else if (algo instanceof AStar) {
                        return new DebugAStar(g, opts.getWeighting(), opts.getTraversalMode(), mg);
                    } else if (algo instanceof DijkstraBidirectionRef) {
                        return new DebugDijkstraBidirection(g, opts.getWeighting(), opts.getTraversalMode(), mg);
                    } else if (algo instanceof Dijkstra) {
                        return new DebugDijkstraSimple(g, opts.getWeighting(), opts.getTraversalMode(), mg);
                    }
                    return algo;
                }
            };
            algoOpts = new AlgorithmOptions(Algorithms.ASTAR_BI, weighting);
        }

        logger.info("locations:" + graph.getNodes() + ", debug:" + debug + ", algoOpts:" + algoOpts);
        mg = new GraphicsWrapper(graph);

        // prepare node quadtree to 'enter' the graph. create a 313*313 grid => <3km
//         this.index = new DebugLocation2IDQuadtree(roadGraph, mg);
        this.index = (LocationIndexTree) hopper.getLocationIndex();
//        this.algo = new DebugDijkstraBidirection(graph, mg);
        // this.algo = new DijkstraBidirection(graph);
//        this.algo = new DebugAStar(graph, mg);
//        this.algo = new AStar(graph);
//        this.algo = new DijkstraSimple(graph);
//        this.algo = new DebugDijkstraSimple(graph, mg);
        infoPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Color.WHITE);
                Rectangle b = infoPanel.getBounds();
                g.fillRect(0, 0, b.width, b.height);
                g.setColor(Color.BLUE);
                g.drawString(latLon, 40, 20);
                g.drawString("scale:" + mg.getScaleX(), 40, 40);
                int w = mainPanel.getBounds().width;
                int h = mainPanel.getBounds().height;
                g.drawString(mg.setBounds(0, w, 0, h).toLessPrecisionString(), 40, 60);

            }
        };


        String[] levels = ((GraphHopperStorage) graph).getProperties().get("levels").split(",");
        levels[0] = levels[0].replace("[", "");
        levels[levels.length - 1] = levels[levels.length - 1].replace("]", "");
        int [] intLevels = new int[levels.length];
        for(int i=0;i< levels.length;i++){
            if(!levels[i].contains(";"))
                intLevels[i]=Integer.parseInt(levels[i].trim());
        }
        Arrays.sort(intLevels);
        for(int i=0;i<intLevels.length/2;i++){
            int temp = intLevels[i];
            intLevels[i]=intLevels[intLevels.length-1-i];
            intLevels[intLevels.length-1-i]=temp;
        }

        settings = new JPanel();
        settings.setLayout(new BoxLayout(settings,BoxLayout.PAGE_AXIS));
        settings.add(new JLabel("Level"));
        for (int level : intLevels
                ) {
            JRadioButton levelButton = new JRadioButton(""+level);
            if((""+level).equals(currentLevel))
                levelButton.setSelected(true);
            allLevelButtons.add(levelButton);
            levelButton.setHorizontalAlignment(0);
            levelButton.addActionListener(new LevelListener(levelButton));
            settings.add(levelButton);
        }

        //add different indoor encoders
        settings.add(new JSeparator());
        settings.add(new JLabel("Encoder"));

        JRadioButton stairsAndElevators = new JRadioButton("Stairs and elevators");
        stairsAndElevators.setSelected(true);
        stairsAndElevators.addActionListener(new EncoderListener(stairsAndElevators,"indoor",hopper));
        settings.add(stairsAndElevators);
        allEncoders.add(stairsAndElevators);

        JRadioButton noStairs = new JRadioButton("No stairs");
        noStairs.addActionListener(new EncoderListener(noStairs,"nostairs",hopper));
        settings.add(noStairs);
        allEncoders.add(noStairs);

        JRadioButton noElevators = new JRadioButton("No elevators");
        noElevators.addActionListener(new EncoderListener(noElevators,"noelevators",hopper));
        settings.add(noElevators);
        allEncoders.add(noElevators);


        mainPanel = new LayeredPanel();

        // TODO make it correct with bitset-skipping too
        final GHBitSet bitset = new GHTBitSet(graph.getNodes());
        mainPanel.addLayer(wayLayer = new WayLayer(graph, bitset));

        mainPanel.addLayer(pathLayer = new DefaultMapLayer() {
            @Override
            public void paintComponent(final Graphics2D g2) {
                if (fromRes == null || toRes == null)
                    return;
                makeTransparent(g2);
                QueryGraph qGraph = new QueryGraph(routingGraph).lookup(fromRes, toRes);
                RoutingAlgorithm algo = algoFactory.createAlgo(qGraph, algoOpts);
                if (algo instanceof DebugAlgo) {
                    ((DebugAlgo) algo).setGraphics2D(g2);
                }

                StopWatch sw = new StopWatch().start();
                logger.info("start searching with " + algo + " from:" + fromRes + " to:" + toRes + " " + weighting);


                if (fromLevel.equals(currentLevel))
                    g2.setColor(edgeOnSameLevel);
                else
                    g2.setColor(edgeOnDifferentLevel);

                mg.plotNode(g2, qGraph.getNodeAccess(), fromRes.getClosestNode(), g2.getColor(), 10, "");
                if (toLevel.equals(currentLevel))
                    g2.setColor(edgeOnSameLevel);
                else
                    g2.setColor(edgeOnDifferentLevel);
                mg.plotNode(g2, qGraph.getNodeAccess(), toRes.getClosestNode(), g2.getColor(), 10, "");
                path = algo.calcPath(fromRes.getClosestNode(), toRes.getClosestNode());
                sw.stop();


                // if directed edges
                if (!path.isFound()) {
                    logger.warn("path not found! direction not valid?");
                    return;
                }

                logger.info("found path in " + sw.getSeconds() + "s with nodes:"
                        + path.calcNodes().size() + ", millis: " + path.getTime()
                        + ", visited nodes:" + algo.getVisitedNodes());
                plotPath(path, g2, 2);
            }

        });

        if (debug) {
            // disable double buffering for debugging drawing - nice! when do we need DebugGraphics then?
            RepaintManager repaintManager = RepaintManager.currentManager(mainPanel);
            repaintManager.setDoubleBufferingEnabled(false);
            mainPanel.setBuffering(false);
        }
    }

    public static void main(String[] strs) throws Exception {
        CmdArgs args = CmdArgs.read(strs);
        GraphHopperOSM hopper = new GraphHopperOSM();
        hopper.init(args).importOrLoad();
        boolean debug = args.getBool("minigraphui.debug", false);
        new MiniGraphUIIndoor(hopper, debug).visualize();
    }

    // for debugging
    private Path calcPath(RoutingAlgorithm algo) {
//        int from = index.findID(50.042, 10.19);
//        int to = index.findID(50.049, 10.23);
//
////        System.out.println("path " + from + "->" + to);
//        return algo.calcPath(from, to);
        // System.out.println(GraphUtility.getNodeInfo(graph, 60139, new DefaultEdgeFilter(new CarFlagEncoder()).direction(false, true)));
        // System.out.println(((GraphStorage) graph).debug(202947, 10));
//        GraphUtility.printInfo(graph, 106511, 10);
        return algo.calcPath(162810, 35120);
    }

    void plotNodeName(Graphics2D g2, int node) {
        double lat = nodeAccess.getLatitude(node);
        double lon = nodeAccess.getLongitude(node);
        mg.plotText(g2, lat, lon, "" + node);
    }

    private Path plotPath(Path tmpPath, Graphics2D g2, int width) {
        if (!tmpPath.isFound()) {
            logger.info("nothing found " + width);
            return tmpPath;
        }

        double prevLat = Double.NaN;
        double prevLon = Double.NaN;
        boolean plotNodes = false;
        IntIndexedContainer nodes = tmpPath.calcNodes();
        if (plotNodes) {
            for (int i = 0; i < nodes.size(); i++) {
                plotNodeName(g2, nodes.get(i));
            }
        }


        int[] levels = new int[2];
        levels[0] = Integer.parseInt(fromLevel);
        levels[1] = Integer.parseInt(toLevel);
        tmpPath.setLevels(levels);

        PointListIndoor pathPoints = (PointListIndoor) tmpPath.calcPoints();

        for (int i = 0; i < pathPoints.getSize(); i++) {
            double lat = pathPoints.getLatitude(i);
            double lon = pathPoints.getLongitude(i);
            int level = pathPoints.getLevel(i);
            width = 4;
            if(Integer.toString(level).equals(currentLevel)) {
                g2.setColor(edgeOnSameLevel);
            }
            else {
                g2.setColor(edgeOnDifferentLevel);
            }

            if (!Double.isNaN(prevLat)) {
                mg.plotEdge(g2, prevLat, prevLon, lat, lon, width);
            } else {
                mg.plot(g2, lat, lon, width);
            }
            prevLat = lat;
            prevLon = lon;
        }
        logger.info("dist:" + tmpPath.getDistance() + ", path points(" + pathPoints.getSize() + ")");
        return tmpPath;
    }

    public void visualize() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    int frameHeight = 800;
                    int frameWidth = 1200;
                    JFrame frame = new JFrame("GraphHopper UI Indoor");
                    frame.setLayout(new BorderLayout());
                    frame.add(mainPanel, BorderLayout.CENTER);
                    frame.add(infoPanel, BorderLayout.NORTH);
                    JPanel settingsPanel = new JPanel();
                    settingsPanel.add(settings);
                    frame.add(settingsPanel,BorderLayout.EAST);

                    infoPanel.setPreferredSize(new Dimension(300, 100));

                    // scale
                    mainPanel.addMouseWheelListener(new MouseWheelListener() {
                        @Override
                        public void mouseWheelMoved(MouseWheelEvent e) {
                            mg.scale(e.getX(), e.getY(), e.getWheelRotation() < 0);
                            repaintRoads();
                        }
                    });


                    MouseAdapter ml = new MouseAdapter() {
                        // for routing:
                        double fromLat, fromLon;
                        boolean fromDone = false;
                        boolean dragging = false;

                        @Override
                        public void mouseClicked(MouseEvent e) {
                            if (!fromDone) {
                                fromLat = mg.getLat(e.getY());
                                fromLon = mg.getLon(e.getX());
                                fromLevel = currentLevel;
                            } else {
                                double toLat = mg.getLat(e.getY());
                                double toLon = mg.getLon(e.getX());
                                toLevel = currentLevel;
                                StopWatch sw = new StopWatch().start();
                                logger.info("start searching from " + fromLat + "," + fromLon
                                        + " to " + toLat + "," + toLon);
                                // get from and to node id

                                fromRes = index.findClosest(fromLat, fromLon, new EdgeFilterIndoor(Integer.parseInt(fromLevel)));
                                toRes = index.findClosest(toLat, toLon, new EdgeFilterIndoor(Integer.parseInt(toLevel)));
                                logger.info("found ids " + fromRes + " -> " + toRes + " in " + sw.stop().getSeconds() + "s");

                                repaintPaths();
                            }

                            fromDone = !fromDone;
                        }

                        @Override
                        public void mouseDragged(MouseEvent e) {
                            dragging = true;
                            fastPaint = true;
                            update(e);
                            updateLatLon(e);
                        }

                        @Override
                        public void mouseReleased(MouseEvent e) {
                            if (dragging) {
                                // update only if mouse release comes from dragging! (at the moment equal to fastPaint)
                                dragging = false;
                                fastPaint = false;
                                update(e);
                            }
                        }

                        public void update(MouseEvent e) {
                            mg.setNewOffset(e.getX() - currentPosX, e.getY() - currentPosY);
                            repaintRoads();
                        }

                        @Override
                        public void mouseMoved(MouseEvent e) {
                            updateLatLon(e);
                        }

                        @Override
                        public void mousePressed(MouseEvent e) {
                            updateLatLon(e);
                        }
                    };
                    mainPanel.addMouseListener(ml);
                    mainPanel.addMouseMotionListener(ml);

                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.setSize(frameWidth + 10, frameHeight + 30);
                    frame.setVisible(true);
                }
            });
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    void updateLatLon(MouseEvent e) {
        latLon = mg.getLat(e.getY()) + "," + mg.getLon(e.getX());
        infoPanel.repaint();
        currentPosX = e.getX();
        currentPosY = e.getY();
    }

    void repaintPaths() {
        pathLayer.repaint();
        mainPanel.repaint();
    }

    void repaintRoads() {
        // avoid threading as there should be no updated to scale or offset while painting 
        // (would to lead to artifacts)
        StopWatch sw = new StopWatch().start();
        pathLayer.repaint();
        wayLayer.repaint();
        mainPanel.repaint();
        logger.info("way painting took " + sw.stop().getSeconds() + " sec");
    }

    public class LevelListener implements ActionListener {
        private JToggleButton levelButton;

        public LevelListener(JToggleButton levelButton) {
            this.levelButton = levelButton;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == levelButton) {
                currentLevel = levelButton.getText();
                repaintRoads();
            }
        }
    }

    public class EncoderListener implements ActionListener{
        private JToggleButton encoderButton;
        private String encoderString;
        private GraphHopper hopper;

        EncoderListener(JToggleButton encoderButton,String encoderString,GraphHopper hopper) {
            this.encoderButton = encoderButton;
            this.encoderString = encoderString;
            this.hopper = hopper;
            final RoutingAlgorithmFactory tmpFactory = hopper.getAlgorithmFactory(map);
            algoFactory = new RoutingAlgorithmFactory() {

                @Override
                public RoutingAlgorithm createAlgo(Graph g, AlgorithmOptions opts) {
                    RoutingAlgorithm algo = tmpFactory.createAlgo(g, opts);
                    if (algo instanceof AStarBidirection) {
                        return new DebugAStarBi(g, opts.getWeighting(), opts.getTraversalMode(), mg).
                                setApproximation(((AStarBidirection) algo).getApproximation());
                    } else if (algo instanceof AStar) {
                        return new DebugAStar(g, opts.getWeighting(), opts.getTraversalMode(), mg);
                    } else if (algo instanceof DijkstraBidirectionRef) {
                        return new DebugDijkstraBidirection(g, opts.getWeighting(), opts.getTraversalMode(), mg);
                    } else if (algo instanceof Dijkstra) {
                        return new DebugDijkstraSimple(g, opts.getWeighting(), opts.getTraversalMode(), mg);
                    }
                    return algo;
                }
            };
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == encoderButton){
                encoder = hopper.getEncodingManager().getEncoder(encoderString);
                weighting = hopper.createWeighting(map, encoder, graph);
                algoOpts = new AlgorithmOptions(Algorithms.ASTAR_BI, weighting);
                repaintPaths();

            }
        }
    }


    public class WayLayer extends DefaultMapLayer {

        Random rand = new Random();
        //String currentLevel;
        Graph graph;
        GHBitSet bitset;

        WayLayer(Graph graph, GHBitSet bitset) {
            this.bitset = bitset;
            this.graph = graph;
        }

        @Override
        public void paintComponent(Graphics2D g2) {
            clearGraphics(g2);
            Rectangle d = getBounds();
            BBox b = mg.setBounds(0, d.width, 0, d.height);
            if (fastPaint) {
                rand.setSeed(0);
                bitset.clear();
            }
            float width = 1.2f;

            AllEdgesIterator edge = graph.getAllEdges();
            while (edge.next()) {
                if (edge instanceof EdgeIteratorIndoor) {
                    EdgeIteratorIndoor edgeIndoor = (EdgeIteratorIndoor) edge;
                    if (!edgeIndoor.getLevel().equals(currentLevel)) {
                        width  = 0.8f;
                        g2.setColor(wayOnDifferentLevel);
                    } else {
                        width = 1.8f;
                        g2.setColor(wayOnSameLevel);
                    }
                }
                if (fastPaint && rand.nextInt(30) > 1)
                    continue;

                int nodeIndex = edge.getBaseNode();
                double lat = nodeAccess.getLatitude(nodeIndex);
                double lon = nodeAccess.getLongitude(nodeIndex);
                int nodeId = edge.getAdjNode();
                double lat2 = nodeAccess.getLatitude(nodeId);
                double lon2 = nodeAccess.getLongitude(nodeId);

                // mg.plotText(g2, lat, lon, "" + nodeIndex);
                if (!b.contains(lat, lon) && !b.contains(lat2, lon2))
                    continue;

                int sum = nodeIndex + nodeId;
                if (fastPaint) {
                    if (bitset.contains(sum))
                        continue;

                    bitset.add(sum);
                }

                mg.plotEdge(g2, lat, lon, lat2, lon2, width);

            }

        }

    }

}
