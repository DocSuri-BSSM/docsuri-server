package com.example.docsuriserver.document.service;

import com.example.docsuriserver.common.DocumentType;
import com.example.docsuriserver.document.entity.ExtractedField;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 테스트/오프라인 개발용 더미 구현. app.ocr.provider=mock 일 때만 활성화된다.
 * GROSS_WEIGHT는 B/L만 다르게 줘서(11,800 vs 12,500) Validation 도메인 테스트 시
 * 불일치 시나리오를 재현할 수 있게 한다.
 */
@Component
@ConditionalOnProperty(name = "app.ocr.provider", havingValue = "mock")
public class MockOcrClient implements OcrClient {

    @Override
    public List<ExtractedField> extract(OcrRequest request) {
        return request.extractFields().stream()
                .map(key -> {
                    String value = sampleValueOf(key, request.documentType());
                    return new ExtractedField(
                            key.toLowerCase(),
                            labelOf(key),
                            value,
                            valueNumberOf(value),
                            unitOf(key),
                            new BigDecimal("0.97"),
                            1);
                })
                .toList();
    }

    private String labelOf(String key) {
        return switch (key) {
            case "PRODUCT_NAME" -> "품명";
            case "QUANTITY" -> "수량";
            case "TOTAL_AMOUNT" -> "총금액";
            case "GROSS_WEIGHT" -> "총중량";
            case "PACKAGE_QTY" -> "포장수량";
            case "UNIT_PRICE" -> "단가";
            default -> key;
        };
    }

    private String sampleValueOf(String key, DocumentType documentType) {
        return switch (key) {
            case "PRODUCT_NAME" -> "FROZEN MACKEREL";
            case "QUANTITY" -> "500";
            case "UNIT_PRICE" -> "25.00";
            case "TOTAL_AMOUNT" -> "12500.00";
            case "GROSS_WEIGHT" -> documentType == DocumentType.BILL_OF_LADING ? "11800" : "12500";
            case "PACKAGE_QTY" -> "100";
            default -> "N/A";
        };
    }

    private BigDecimal valueNumberOf(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String unitOf(String key) {
        return switch (key) {
            case "QUANTITY", "PACKAGE_QTY" -> "CTN";
            case "TOTAL_AMOUNT", "UNIT_PRICE" -> "USD";
            case "GROSS_WEIGHT" -> "KG";
            default -> null;
        };
    }
}
