package com.example.docsuriserver.document.entity;

import com.example.docsuriserver.common.DocumentType;

import java.util.List;
import java.util.UUID;

public record ExtractedDocument(
        UUID documentId,
        DocumentType documentType,
        List<ExtractedField> fields
) {
}
