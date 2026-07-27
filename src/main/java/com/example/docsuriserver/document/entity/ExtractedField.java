package com.example.docsuriserver.document.entity;

import java.math.BigDecimal;

/**
 * jsonb 컬럼(document_parse_jobs.extraction_documents) 안에 저장되는 값 객체.
 * valueNumber: "12,500 kg" 같은 원본 문자열을 검증 단계에서 매번 파싱하지 않도록,
 * 숫자로 해석 가능하면 OCR 단계에서 한 번만 채워둔다. 해석 불가하면 null.
 */
public record ExtractedField(
        String fieldKey,
        String label,
        String value,
        BigDecimal valueNumber,
        String unit,
        BigDecimal confidence,
        Integer page
) {
}
