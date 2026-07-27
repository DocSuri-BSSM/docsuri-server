package com.example.docsuriserver.document.service;

import com.example.docsuriserver.common.ExternalApiException;
import com.example.docsuriserver.document.entity.ExtractedField;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Gemini(멀티모달 LLM)가 문서 이미지 1장을 직접 읽어 지정된 필드를 추출한다.
 * 문서 간 교차검증/산술검증은 하지 않는다 — 그건 별도 단계의 책임.
 *
 * 설계서(04-AI-INTEGRATION.md 7절)는 이번 단계에 실제 OCR 엔진을 붙이지 말라고 규정하지만,
 * 개발 속도를 위해 이 구현체를 기본으로 유지하기로 결정했다 (DECISIONS.md 1절 참고).
 */
@Component
@ConditionalOnProperty(name = "app.ocr.provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiOcrClient implements OcrClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String model;
    private final String apiKey;

    public GeminiOcrClient(RestClient geminiRestClient,
                            @Value("${app.gemini.api-key}") String apiKey,
                            @Value("${app.gemini.model.ocr:gemini-2.5-flash}") String model) {
        this.restClient = geminiRestClient;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public List<ExtractedField> extract(OcrRequest request) {
        String responseBody = restClient.post()
                .uri("/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildRequestBody(request))
                .retrieve()
                .body(String.class);

        return parseFields(responseBody);
    }

    private String buildRequestBody(OcrRequest request) {
        Map<String, Object> inlineData = Map.of(
                "mimeType", request.contentType(),
                "data", Base64.getEncoder().encodeToString(request.content()));
        Map<String, Object> imagePart = Map.of("inlineData", inlineData);
        Map<String, Object> textPart = Map.of("text", buildPromptText(request));
        Map<String, Object> content = Map.of("parts", List.of(imagePart, textPart));

        Map<String, Object> fieldSchema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "fieldKey", Map.of("type", "STRING"),
                        "label", Map.of("type", "STRING"),
                        "value", Map.of("type", "STRING"),
                        "valueNumber", Map.of("type", "NUMBER"),
                        "unit", Map.of("type", "STRING"),
                        "confidence", Map.of("type", "NUMBER"),
                        "page", Map.of("type", "INTEGER")),
                "required", List.of("fieldKey", "label", "value", "confidence"));
        Map<String, Object> responseSchema = Map.of("type", "ARRAY", "items", fieldSchema);

        Map<String, Object> generationConfig = Map.of(
                "responseMimeType", "application/json",
                "responseSchema", responseSchema);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(content),
                "generationConfig", generationConfig);

        try {
            return objectMapper.writeValueAsString(requestBody);
        } catch (Exception e) {
            throw new ExternalApiException("Gemini 요청 본문 생성 실패", e);
        }
    }

    private String buildPromptText(OcrRequest request) {
        return """
                당신은 무역서류 OCR 보조원입니다. 첨부된 문서 이미지 1장에서 아래 필드만 정확히 추출하세요.

                - 문서 종류: %s
                - OCR 언어: %s
                - 추출할 필드 키: %s

                규칙:
                - 각 필드 키에 대해 문서에 실제로 적힌 값만 그대로 추출하세요. 추측하거나 계산하지 마세요.
                - value는 문서에 적힌 원본 문자열 그대로(단위 포함 가능) 채우세요. 값이 숫자로 해석 가능하면 콤마와 단위를 제외한 순수 숫자를 valueNumber에도 채우세요. 숫자가 아니면 valueNumber는 생략하세요.
                - 문서에 해당 필드가 없으면 value를 빈 문자열로, confidence를 0으로 설정하세요.
                - 다른 문서와 비교하거나 검증하지 마세요. 이 문서 1장의 내용만 그대로 읽으세요.
                - confidence는 0.0~1.0 사이 값으로, 실제 인식 확신도를 반영하세요.
                - 모든 텍스트는 한국어로 작성하세요.
                """.formatted(request.documentType(), request.ocrLanguage(), String.join(", ", request.extractFields()));
    }

    private List<ExtractedField> parseFields(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asString();
            ExtractedField[] fields = objectMapper.readValue(text, ExtractedField[].class);
            return List.of(fields);
        } catch (Exception e) {
            throw new ExternalApiException("Gemini 응답 파싱 실패: " + responseBody, e);
        }
    }
}
