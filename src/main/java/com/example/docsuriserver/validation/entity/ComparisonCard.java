package com.example.docsuriserver.validation.entity;

import com.example.docsuriserver.common.DocumentType;

public record ComparisonCard(
        DocumentType docType,
        String docDisplayName,
        String fieldName,
        String value,
        boolean isMismatch,
        boolean isMissing
) {
}
