package com.example.docsuriserver.ai;

import com.example.docsuriserver.common.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gemini 호출의 단일 진입점. 다른 도메인 서비스는 이 클래스만 호출한다.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String reasoningModel;
    private final String fastModel;
    private final int maxRetries;

    public GeminiClient(RestClient geminiRestClient,
                         @Value("${app.gemini.api-key}") String apiKey,
                         @Value("${app.gemini.model.reasoning}") String reasoningModel,
                         @Value("${app.gemini.model.fast}") String fastModel,
                         @Value("${app.gemini.max-retries:2}") int maxRetries) {
        this.restClient = geminiRestClient;
        this.apiKey = apiKey;
        this.reasoningModel = reasoningModel;
        this.fastModel = fastModel;
        this.maxRetries = maxRetries;
    }

    public JsonNode generate(GeminiRequest request) {
        String model = switch (request.modelTier()) {
            case REASONING -> reasoningModel;
            case FAST -> fastModel;
        };
        String requestBody = buildRequestBody(request);
        log.info("Gemini 요청 model={} temperature={} body={}", model, request.temperature(), requestBody);

        int attempt = 0;
        while (true) {
            try {
                String responseBody = restClient.post()
                        .uri("/{model}:generateContent", model)
                        .header("x-goog-api-key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);
                log.info("Gemini 응답 model={} body={}", model, responseBody);
                return GeminiResponse.extractJson(objectMapper, responseBody);
            } catch (HttpClientErrorException e) {
                // 4xx는 같은 요청이면 같은 이유로 실패하므로 재시도하지 않는다.
                throw new ExternalApiException("Gemini 요청이 거부되었습니다.", e);
            } catch (RestClientException e) {
                attempt++;
                if (attempt > maxRetries) {
                    throw new ExternalApiException("Gemini 호출이 반복적으로 실패했습니다.", e);
                }
                log.warn("Gemini 호출 실패, {}번째 재시도 예정", attempt, e);
                backoff(attempt);
            }
        }
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(500L * (1L << attempt));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ExternalApiException("Gemini 재시도 대기 중 인터럽트가 발생했습니다.", ie);
        }
    }

    private String buildRequestBody(GeminiRequest request) {
        Map<String, Object> systemInstruction = Map.of("parts", List.of(Map.of("text", request.systemInstruction())));
        Map<String, Object> userContent = Map.of("parts", List.of(Map.of("text", request.userPrompt())));

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", request.temperature());
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("responseSchema", request.responseSchema());

        Map<String, Object> body = Map.of(
                "systemInstruction", systemInstruction,
                "contents", List.of(userContent),
                "generationConfig", generationConfig);

        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new ExternalApiException("Gemini 요청 본문 생성 실패", e);
        }
    }
}
