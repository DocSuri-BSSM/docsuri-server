package com.example.docsuriserver.ai.schema;

import java.util.List;
import java.util.Map;

public final class HsCodeRecommendationSchema {

    private HsCodeRecommendationSchema() {
    }

    public static Map<String, Object> schema() {
        Map<String, Object> candidateItem = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "rank", Map.of("type", "INTEGER"),
                        "hs_code", Map.of("type", "STRING"),
                        "confidence", Map.of("type", "NUMBER"),
                        "reason", Map.of("type", "STRING")),
                "required", List.of("rank", "hs_code", "confidence", "reason"));

        Map<String, Object> candidatesArray = Map.of("type", "ARRAY", "items", candidateItem);

        return Map.of(
                "type", "OBJECT",
                "properties", Map.of("candidates", candidatesArray),
                "required", List.of("candidates"));
    }
}
