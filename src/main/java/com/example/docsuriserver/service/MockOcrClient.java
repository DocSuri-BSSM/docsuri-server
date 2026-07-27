package com.example.docsuriserver.service;

import com.example.docsuriserver.entity.Document;
import com.example.docsuriserver.entity.ExtractedField;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 프론트 연동용 더미 구현. application.yaml 의 profile 로 실제 구현과 교체한다.
 */
@Component
@Profile({"local", "default"})
public class MockOcrClient implements OcrClient {

    @Override
    public List<ExtractedField> extract(Document document, String ocrLanguage, List<String> extractFields) {
        return extractFields.stream()
                .map(key -> new ExtractedField(
                        key.toLowerCase(),
                        labelOf(key),
                        sampleValueOf(key),
                        unitOf(key),
                        new BigDecimal("0.97"),
                        1))
                .toList();
    }

    private String labelOf(String key) {
        return switch (key) {
            case "PRODUCT_NAME" -> "품명";
            case "QUANTITY" -> "수량";
            case "TOTAL_AMOUNT" -> "총금액";
            case "GROSS_WEIGHT" -> "총중량";
            default -> key;
        };
    }

    private String sampleValueOf(String key) {
        return switch (key) {
            case "PRODUCT_NAME" -> "FROZEN MACKEREL";
            case "QUANTITY" -> "500";
            case "TOTAL_AMOUNT" -> "12500.00";
            case "GROSS_WEIGHT" -> "1250";
            default -> "N/A";
        };
    }

    private String unitOf(String key) {
        return switch (key) {
            case "QUANTITY" -> "CTN";
            case "TOTAL_AMOUNT" -> "USD";
            case "GROSS_WEIGHT" -> "KG";
            default -> null;
        };
    }
}
