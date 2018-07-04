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
     package com.graphhopper.routing;

     import com.carrotsearch.hppc.IntArrayList;
     import com.carrotsearch.hppc.IntIndexedContainer;
     import com.graphhopper.coll.GHIntArrayList;
     import com.graphhopper.routing.util.FlagEncoder;
     import com.graphhopper.routing.weighting.Weighting;
     import com.graphhopper.storage.Graph;
     import com.graphhopper.storage.IndoorExtension;
     import com.graphhopper.storage.NodeAccess;
     import com.graphhopper.storage.SPTEntry;
     import com.graphhopper.util.*;
     import com.graphhopper.util.details.PathDetail;
     import com.graphhopper.util.details.PathDetailsBuilder;
     import com.graphhopper.util.details.PathDetailsBuilderFactory;
     import com.graphhopper.util.details.PathDetailsFromEdges;
     import org.slf4j.Logger;
     import org.slf4j.LoggerFactory;

     import java.util.*;

     /**
      * Stores the nodes for the found path of an algorithm. It additionally needs the edgeIds to make
      * edge determination faster and less complex as there could be several edges (u,v) especially for
      * graphs with shortcuts.
      * <p>
      *
      * @author Peter Karich
      * @author Ottavio Campana
      * @author jan soe
      */
     public class PathIndoor extends Path {
         final StopWatch extractSW = new StopWatch("extract");
         private final Logger logger = LoggerFactory.getLogger(getClass());
         protected Graph graph;
         protected double distance;
         // we go upwards (via SPTEntry.parent) from the goal node to the origin node
         protected boolean reverseOrder = true;
         protected long time;
         /**
          * Shortest path tree entry
          */
         protected SPTEntry sptEntry;
         protected int endNode = -1;
         protected Weighting weighting;
         private IndoorExtension indoorExtension;
         private List<String> description;
         private FlagEncoder encoder;
         private boolean found;
         private int fromNode = -1;
         private GHIntArrayList edgeIds;
         private double weight;
         private NodeAccess nodeAccess;

         public PathIndoor(Graph graph, Weighting weighting) {
             super(graph, weighting);
             this.weight = Double.MAX_VALUE;
             this.graph = graph;
             this.nodeAccess = graph.getNodeAccess();
             this.weighting = weighting;
             this.encoder = weighting.getFlagEncoder();
             this.edgeIds = new GHIntArrayList();
             this.indoorExtension = (IndoorExtension) graph.getExtension();
         }

         /**
          * Populates an unextracted path instances from the specified path p.
          */
         PathIndoor(PathIndoor p) {
             this(p.graph, p.weighting);
             weight = p.weight;
             edgeIds = new GHIntArrayList(p.edgeIds);
             sptEntry = p.sptEntry;
         }


         @Override
         public Path setFromNode(int fromNode) {
             super.setFromNode(fromNode);
             this.fromNode = fromNode;
             return this;
         }

         /**
          * @return the first node of this Path.
          */
         private int getFromNode() {
             if (fromNode < 0)
                 throw new IllegalStateException("Call extract() before retrieving fromNode");

             return fromNode;
         }

         @Override
         public Path setEndNode(int endNode) {
             super.setEndNode(endNode);
             this.endNode = endNode;
             return this;
         }

         protected void addEdge(int edge) {
             super.addEdge(edge);
             edgeIds.add(edge);
         }

         /**
          * @return the uncached node indices of the all nodes in this path.
          */
         public Map<Integer,Integer> getLevelMap() {
             Map<Integer,Integer> levelMap = new HashMap<>();
             for (int i = 0; i < edgeIds.size(); i++) {
                 int node = edgeIds.get(i);
                 int level = indoorExtension.getLevel(node);
                 levelMap.put(node,level);
             }
             return levelMap;

         }

         /**
          * Iterates over all edges in this path sorted from start to end and calls the visitor callback
          * for every edge.
          * <p>
          *
          * @param visitor callback to handle every edge. The edge is decoupled from the iterator and can
          *                be stored.
          */
         private void forEveryEdge(EdgeVisitor visitor) {
             int tmpNode = getFromNode();
             int len = edgeIds.size();
             int prevEdgeId = EdgeIterator.NO_EDGE;
             for (int i = 0; i < len; i++) {
                 EdgeIteratorState edgeBase = graph.getEdgeIteratorState(edgeIds.get(i), tmpNode);
                 if (edgeBase == null)
                     throw new IllegalStateException("Edge " + edgeIds.get(i) + " was empty when requested with node " + tmpNode
                             + ", array index:" + i + ", edges:" + edgeIds.size());

                 tmpNode = edgeBase.getBaseNode();
                 // more efficient swap, currently not implemented for virtual edges: visitor.next(edgeBase.detach(true), i);
                 edgeBase = graph.getEdgeIteratorState(edgeBase.getEdge(), tmpNode);
                 visitor.next(edgeBase, i, prevEdgeId);

                 prevEdgeId = edgeBase.getEdge();
             }
             visitor.finish();
         }

         @Override
         public PointList calcPoints() {
             final PointList points = new PointListIndoor(edgeIds.size() + 1, nodeAccess.is3D());
             if (edgeIds.isEmpty()) {
                 if (isFound()) {
                     points.add(graph.getNodeAccess(), endNode);
                 }
                 return points;
             }

             List<EdgeIteratorState> edges = calcEdges();
             int tmpNode = getFromNode();
             points.add(nodeAccess, tmpNode);
             for(int i=0;i<edges.size();i++){
                 System.out.println("Edge: "+edges.get(i).toString());
             }
             return super.calcPoints();

         }
     }
