package com.example.docsuriserver.entity;

import java.util.List;
import java.util.UUID;

public record ExtractedDocument(
        UUID documentId,
        DocumentType documentType,
        List<ExtractedField> fields
) {
}
