package jadelab2;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class CityMap {
    private Map<String, Map<String, Integer>> cityMap;

    public void initializeMap() {
        cityMap = new HashMap<>();

        // Inicjalizacja mapy miasta
        cityMap.put("P1", Map.of("P2", 5, "P3", 10));
        cityMap.put("P2", Map.of("P1", 5, "P4", 2));
        cityMap.put("P3", Map.of("P1", 10, "P4", 1));
        cityMap.put("P4", Map.of("P2", 2, "P3", 1, "P5", 3));
        cityMap.put("P5", Map.of("P4", 3, "P6", 6));
        cityMap.put("P6", Map.of("P5", 6, "P7", 2));
        cityMap.put("P7", Map.of("P6", 2, "P8", 4));
        cityMap.put("P8", Map.of("P7", 4));
        cityMap.put("P9", Map.of("P8", 4,"P1",1));
    }

    public Map<String, Map<String, Integer>> getCityMap() {
        return cityMap;
    }

    public Map<String, Integer> shortestPathsFrom(String start) {
        Map<String, Integer> distances = new HashMap<>();
        PriorityQueue<Map.Entry<String, Integer>> queue = new PriorityQueue<>(Map.Entry.comparingByValue());

        cityMap.keySet().forEach(v -> distances.put(v, Integer.MAX_VALUE));
        distances.put(start, 0);
        queue.add(Map.entry(start, 0));

        while (!queue.isEmpty()) {
            Map.Entry<String, Integer> current = queue.poll();
            String currentPoint = current.getKey();
            Integer currentDistance = current.getValue();

            cityMap.get(currentPoint).forEach((neighbor, weight) -> {
                int newDist = currentDistance + weight;
                if (newDist < distances.get(neighbor)) {
                    distances.put(neighbor, newDist);
                    queue.add(Map.entry(neighbor, newDist));
                }
            });
        }

        return distances;
    }
}