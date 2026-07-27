package com.example.docsuriserver.ai.schema;

import java.util.List;
import java.util.Map;

public final class ValidationNarrativeSchema {

    private ValidationNarrativeSchema() {
    }

    public static Map<String, Object> schema() {
        Map<String, Object> narrativeItem = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "rule", Map.of("type", "STRING"),
                        "title", Map.of("type", "STRING"),
                        "subtitle", Map.of("type", "STRING"),
                        "cause", Map.of("type", "STRING"),
                        "risk_warning", Map.of("type", "STRING")),
                "required", List.of("rule", "title", "subtitle", "cause", "risk_warning"));

        Map<String, Object> narrativesArray = Map.of("type", "ARRAY", "items", narrativeItem);

        return Map.of(
                "type", "OBJECT",
                "properties", Map.of("narratives", narrativesArray),
                "required", List.of("narratives"));
    }
}
