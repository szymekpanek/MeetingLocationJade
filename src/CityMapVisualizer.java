package jadelab2;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.nio.dot.DOTExporter;
import org.jgrapht.util.SupplierUtil;

import java.io.StringWriter;
import java.util.Map;
import java.util.function.Supplier;

public class CityMapVisualizer {

    public static void visualizeMap(Map<String, Map<String, Integer>> cityMap) {
        Graph<String, DefaultEdge> graph = createGraph(cityMap);
        DOTExporter<String, DefaultEdge> exporter = new DOTExporter<>();

        StringWriter writer = new StringWriter();
        exporter.exportGraph(graph, writer);

        System.out.println(writer.toString());
    }

    private static Graph<String, DefaultEdge> createGraph(Map<String, Map<String, Integer>> cityMap) {
        Supplier<String> vSupplier = SupplierUtil.createStringSupplier();
        Supplier<DefaultEdge> eSupplier = SupplierUtil.createDefaultEdgeSupplier();

        Graph<String, DefaultEdge> graph = new DefaultUndirectedWeightedGraph<>(vSupplier, eSupplier);

        // Add vertices
        cityMap.keySet().forEach(graph::addVertex);

        // Add edges
        cityMap.forEach((source, edges) -> {
            edges.forEach((target, weight) -> {
                graph.addEdge(source, target);
                graph.setEdgeWeight(source, target, weight);
            });
        });

        return graph;
    }
}