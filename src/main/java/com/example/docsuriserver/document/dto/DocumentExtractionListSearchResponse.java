package com.example.docsuriserver.document.dto;

import com.example.docsuriserver.document.entity.ExtractedDocument;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record DocumentExtractionListSearchResponse(
        List<DocumentItem> documents
) {

    public record DocumentItem(
            UUID documentId,
            String documentType,
            List<FieldItem> fields
    ) {
    }

    public record FieldItem(
            String fieldKey,
            String label,
            String value,
            BigDecimal valueNumber,
            String unit,
            BigDecimal confidence,
            Integer page
    ) {
    }

    public static DocumentExtractionListSearchResponse from(List<ExtractedDocument> extracted) {
        List<DocumentItem> items = extracted.stream()
                .map(doc -> new DocumentItem(
                        doc.documentId(),
                        doc.documentType().name(),
                        doc.fields().stream()
                                .map(f -> new FieldItem(
                                        f.fieldKey(), f.label(), f.value(), f.valueNumber(),
                                        f.unit(), f.confidence(), f.page()))
                                .toList()))
                .toList();
        return new DocumentExtractionListSearchResponse(items);
    }
}
