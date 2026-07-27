package com.example.docsuriserver.ai;

import java.util.Map;

public record GeminiRequest(
        String systemInstruction,
        String userPrompt,
        Map<String, Object> responseSchema,
        double temperature,
        GeminiModelTier modelTier
) {
}
