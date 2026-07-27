package com.example.docsuriserver.ai.schema;

import java.util.List;
import java.util.Map;

public final class CorrectionRequestSchema {

    private CorrectionRequestSchema() {
    }

    public static Map<String, Object> schema() {
        Map<String, Object> variableItem = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "variable_key", Map.of("type", "STRING"),
                        "label", Map.of("type", "STRING"),
                        "value", Map.of("type", "STRING"),
                        "required", Map.of("type", "BOOLEAN")),
                "required", List.of("variable_key", "label", "value", "required"));

        return Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "title", Map.of("type", "STRING"),
                        "content", Map.of("type", "STRING"),
                        "variables", Map.of("type", "ARRAY", "items", variableItem)),
                "required", List.of("title", "content", "variables"));
    }
}
