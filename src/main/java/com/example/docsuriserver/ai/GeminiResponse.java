package com.example.docsuriserver.ai;

import com.example.docsuriserver.common.ExternalApiException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** generateContent 응답 봉투에서 실제 JSON 페이로드(candidates[0].content.parts[0].text)를 꺼낸다. */
final class GeminiResponse {

    private GeminiResponse() {
    }

    static JsonNode extractJson(ObjectMapper objectMapper, String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String text = root.path("candidates").path(0).path("content").path("parts").path(0)
                    .path("text").asString();
            return objectMapper.readTree(text);
        } catch (Exception e) {
            throw new ExternalApiException("Gemini 응답 파싱 실패: " + responseBody, e);
        }
    }
}
