package com.example.docsuriserver.ai.schema;

import java.util.List;
import java.util.Map;

public final class HsCodeJustificationSchema {

    private HsCodeJustificationSchema() {
    }

    public static Map<String, Object> schema() {
        return Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "title", Map.of("type", "STRING"),
                        "content", Map.of("type", "STRING"),
                        "legal_basis", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"))),
                "required", List.of("title", "content", "legal_basis"));
    }
}
