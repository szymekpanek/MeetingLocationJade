package jadelab2;

import java.util.HashMap;
import java.util.Map;

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
    }

    public Map<String, Map<String, Integer>> getCityMap() {
        return cityMap;
    }
}

