package com.example.docsuriserver.entity;

import java.math.BigDecimal;

/**
 * jsonb 컬럼(document_parse_jobs.extraction_documents) 안에 저장되는 값 객체.
 */
public record ExtractedField(
        String fieldKey,
        String label,
        String value,
        String unit,
        BigDecimal confidence,
        Integer page
) {
}
