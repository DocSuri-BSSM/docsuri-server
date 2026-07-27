package com.example.docsuriserver;

import com.example.docsuriserver.ai.GeminiClient;
import com.example.docsuriserver.ai.GeminiRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 05-TASKS.md T9: 업로드 → 파싱 → 검증 → 정정요청 → export 전체 플로우를 1건으로 검증한다.
 * OCR은 app.ocr.provider=mock으로 대체하고, GeminiClient는 스텁으로 대체한다 —
 * 이 스텁이 등급/카운트/신호등에 관여하지 않는다는 것 자체가 "판정이 LLM에 의존하지 않는다"는 증거다
 * (ValidationEngineTest에서 이미 별도로 검증된 사실을 여기서는 end-to-end로 재확인한다).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "app.ocr.provider=mock",
        "app.gemini.api-key=test-dummy-key"
})
class FullFlowIntegrationTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private GeminiClient geminiClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RestClient client;

    private RestClient client() {
        if (client == null) {
            client = RestClient.builder().baseUrl("http://localhost:" + port).build();
        }
        return client;
    }

    @Test
    void fullFlow_uploadToExport() {
        stubGeminiClient();

        // 1. 업로드
        JsonNode uploadData = postMultipart();
        String sessionId = uploadData.path("session_id").asString();
        assertThat(sessionId).isNotBlank();

        // 2. 파싱 시작
        JsonNode parseData = postJson("/document-sessions/" + sessionId + "/parse", """
                {"ocr_language":"kor+eng","extract_fields":{"INVOICE":["GROSS_WEIGHT","PACKAGE_QTY"],"BILL_OF_LADING":["GROSS_WEIGHT","PACKAGE_QTY"]}}
                """).path("data");
        assertThat(parseData.path("parse_job_id").asString()).isNotBlank();

        // 3. 파싱 완료 대기
        pollUntil(() -> getJson("/document-sessions/" + sessionId + "/parse-status").path("data").path("status").asString(),
                "COMPLETED");

        // 4. 검증 시작
        JsonNode validationData = postJson("/document-sessions/" + sessionId + "/validation", """
                {"rules":["GROSS_WEIGHT_MATCH","PACKAGE_QTY_MATCH"],"weight_tolerance_percent":0.5}
                """).path("data");
        assertThat(validationData.path("validation_run_id").asString()).isNotBlank();

        // 5. 검증 완료 대기 + 결과 확인
        pollUntil(() -> getJson("/document-sessions/" + sessionId + "/validation-result").path("data").path("status").asString(),
                "COMPLETED");
        JsonNode validationResult = getJson("/document-sessions/" + sessionId + "/validation-result").path("data");
        assertThat(validationResult.path("overall_signal").asString()).isEqualTo("RED"); // GROSS_WEIGHT 12500 vs 11800
        assertThat(validationResult.path("error_count").asInt()).isGreaterThanOrEqualTo(1);
        String validationRunId = validationData.path("validation_run_id").asString();

        // 6. 정정 요청서 생성
        String correctionBody = objectMapper.writeValueAsString(Map.of(
                "validation_run_id", validationRunId,
                "output_language", "KO"));
        JsonNode correctionData = postJson("/document-sessions/" + sessionId + "/correction-requests", correctionBody).path("data");
        String correctionRequestId = correctionData.path("correction_request_id").asString();
        assertThat(correctionRequestId).isNotBlank();

        // 7. 조회 — 플레이스홀더 변수 확인
        JsonNode correction = getJson("/correction-requests/" + correctionRequestId).path("data");
        assertThat(correction.path("status").asString()).isEqualTo("DRAFT");
        assertThat(correction.path("variables")).anyMatch(v -> v.path("variable_key").asString().equals("company_name"));

        // 8. PATCH — 필수 변수 채우기
        ResponseEntity<String> patchResponse = client().patch()
                .uri("/correction-requests/" + correctionRequestId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"variables":[{"variable_key":"company_name","value":"ABC Shipping Co."}]}
                        """)
                .retrieve()
                .toEntity(String.class);
        assertThat(patchResponse.getStatusCode().value()).isEqualTo(204);

        JsonNode afterPatch = getJson("/correction-requests/" + correctionRequestId).path("data");
        assertThat(afterPatch.path("status").asString()).isEqualTo("CONFIRMED");
        assertThat(afterPatch.path("content").asString()).contains("ABC Shipping Co.");

        // 9. export (PDF) — 한글 깨짐 없이 생성되는지 확인
        JsonNode exportData = postJson("/correction-requests/" + correctionRequestId + "/export", """
                {"format":"PDF","include_validation_report":true}
                """).path("data");
        String downloadUrl = exportData.path("download_url").asString();
        assertThat(downloadUrl).isNotBlank();

        byte[] pdfBytes = client().get().uri(downloadUrl).retrieve().body(byte[].class);
        assertThat(pdfBytes).isNotEmpty();

        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("ABC Shipping Co.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 10. export 이후 상태는 EXPORTED, 재수정 불가(409)
        JsonNode afterExport = getJson("/correction-requests/" + correctionRequestId).path("data");
        assertThat(afterExport.path("status").asString()).isEqualTo("EXPORTED");

        ResponseEntity<String> patchAfterExport = client().patch()
                .uri("/correction-requests/" + correctionRequestId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"title":"안될 것"}
                        """)
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);
        assertThat(patchAfterExport.getStatusCode().value()).isEqualTo(409);
    }

    private void stubGeminiClient() {
        when(geminiClient.generate(any(GeminiRequest.class))).thenAnswer(invocation -> {
            GeminiRequest req = invocation.getArgument(0);
            if (req.userPrompt().contains("<FINDINGS>")) {
                return objectMapper.readTree("""
                        {"narratives":[
                          {"rule":"GROSS_WEIGHT_MATCH","title":"총중량 불일치","subtitle":"Invoice 12,500kg vs B/L 11,800kg","cause":"B/L 기재 오류로 추정됩니다.","risk_warning":"통관 보류·과태료 사유가 될 수 있습니다."},
                          {"rule":"PACKAGE_QTY_MATCH","title":"포장수량 일치","subtitle":"100 CTN 동일","cause":"모든 서류가 일치합니다.","risk_warning":"해당 없음"}
                        ]}
                        """);
            }
            if (req.userPrompt().contains("<ERROR_ISSUES>")) {
                return objectMapper.readTree("""
                        {"title":"B/L 총중량 정정 요청서","content":"[업체명 입력 필요] 귀중\\n\\nB/L 총중량의 정정을 요청드립니다.","variables":[
                          {"variable_key":"company_name","label":"수신 업체명","value":"[업체명 입력 필요]","required":true}
                        ]}
                        """);
            }
            throw new IllegalStateException("예상하지 못한 Gemini 호출: " + req.userPrompt());
        });
    }

    private JsonNode postMultipart() {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("invoice_file", pdfPart("invoice.pdf"));
        body.add("bill_of_lading_file", pdfPart("bl.pdf"));

        ResponseEntity<String> response = client().post()
                .uri("/documents")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .toEntity(String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        return objectMapper.readTree(response.getBody()).path("data");
    }

    private HttpEntity<ByteArrayResource> pdfPart(String fileName) {
        ByteArrayResource resource = new ByteArrayResource("dummy pdf bytes".getBytes()) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.APPLICATION_PDF);
        return new HttpEntity<>(resource, fileHeaders);
    }

    private JsonNode postJson(String path, String body) {
        ResponseEntity<String> response = client().post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("POST %s failed: %s", path, response.getBody())
                .isTrue();
        return objectMapper.readTree(response.getBody());
    }

    private JsonNode getJson(String path) {
        ResponseEntity<String> response = client().get().uri(path).retrieve().toEntity(String.class);
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("GET %s failed: %s", path, response.getBody())
                .isTrue();
        return objectMapper.readTree(response.getBody());
    }

    private void pollUntil(java.util.function.Supplier<String> statusSupplier, String expected) {
        long deadline = System.currentTimeMillis() + 15_000;
        String last = null;
        while (System.currentTimeMillis() < deadline) {
            last = statusSupplier.get();
            if (expected.equals(last) || "FAILED".equals(last)) {
                break;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        assertThat(last).isEqualTo(expected);
    }
}
