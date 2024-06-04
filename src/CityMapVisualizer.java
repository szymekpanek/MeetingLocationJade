package jadelab2;

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.util.mxCellRenderer;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class CityMapVisualizer {

    public static void saveGraphAsImage(Graph<String, DefaultEdge> graph, String filePath, Set<String> agentLocations, String meetingPoint) throws IOException {
        com.mxgraph.view.mxGraph mxGraph = new com.mxgraph.view.mxGraph();
        Object parent = mxGraph.getDefaultParent();
        mxGraph.getModel().beginUpdate();
        try {
            Map<String, Object> vertexMap = new HashMap<>();
            for (String vertex : graph.vertexSet()) {
                Color fillColor = agentLocations.contains(vertex) ? Color.RED : (vertex.equals(meetingPoint) ? Color.GREEN : Color.WHITE);
                Object v = mxGraph.insertVertex(parent, null, vertex, 0, 0, 80, 30, "fillColor=" + colorToHex(fillColor));
                vertexMap.put(vertex, v);
            }
            for (DefaultEdge edge : graph.edgeSet()) {
                String source = graph.getEdgeSource(edge);
                String target = graph.getEdgeTarget(edge);
                mxGraph.insertEdge(parent, null, String.valueOf(graph.getEdgeWeight(edge)), vertexMap.get(source), vertexMap.get(target));
            }
        } finally {
            mxGraph.getModel().endUpdate();
        }

        // Layout for vertices
        mxCircleLayout layout = new mxCircleLayout(mxGraph);
        layout.execute(mxGraph.getDefaultParent());

        // Generate a BufferedImage from the mxGraph
        BufferedImage image = mxCellRenderer.createBufferedImage(mxGraph, null, 2, java.awt.Color.WHITE, true, null);

        // Save the image as a PNG file
        ImageIO.write(image, "PNG", new File(filePath));

        System.out.println("Graph saved as " + filePath);
    }

    private static String colorToHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    public static Graph<String, DefaultEdge> createGraphFromCityMap(Map<String, Map<String, Integer>> cityMap) {
        Graph<String, DefaultEdge> graph = new SimpleWeightedGraph<>(DefaultEdge.class);

        for (String point : cityMap.keySet()) {
            graph.addVertex(point);
        }

        for (Entry<String, Map<String, Integer>> entry : cityMap.entrySet()) {
            String startPoint = entry.getKey();
            for (Entry<String, Integer> connectedPoint : entry.getValue().entrySet()) {
                graph.addEdge(startPoint, connectedPoint.getKey());
                graph.setEdgeWeight(startPoint, connectedPoint.getKey(), connectedPoint.getValue());
            }
        }

        return graph;
    }
}