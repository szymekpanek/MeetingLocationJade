package jadelab2;

import java.util.*;

public class DijkstraAlgorithm {
    public static Map<String, Integer> computeShortestPaths(String start, Map<String, Map<String, Integer>> graph) {
        Map<String, Integer> distances = new HashMap<>();
        PriorityQueue<String> nodes = new PriorityQueue<>(Comparator.comparingInt(distances::get));
        Map<String, String> previous = new HashMap<>();

        for (String node : graph.keySet()) {
            if (node.equals(start)) {
                distances.put(node, 0);
            } else {
                distances.put(node, Integer.MAX_VALUE);
            }
            nodes.add(node);
        }

        while (!nodes.isEmpty()) {
            String smallest = nodes.poll();
            if (distances.get(smallest) == Integer.MAX_VALUE) break;

            for (Map.Entry<String, Integer> neighbor : graph.get(smallest).entrySet()) {
                int alt = distances.get(smallest) + neighbor.getValue();
                if (alt < distances.get(neighbor.getKey())) {
                    distances.put(neighbor.getKey(), alt);
                    previous.put(neighbor.getKey(), smallest);
                    nodes.add(neighbor.getKey());
                }
            }
        }

        return distances;
    }
}
