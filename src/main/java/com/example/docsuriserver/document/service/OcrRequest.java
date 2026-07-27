package com.example.docsuriserver.document.service;

import com.example.docsuriserver.common.DocumentType;

import java.util.List;

public record OcrRequest(
        byte[] content,
        String contentType,
        DocumentType documentType,
        String ocrLanguage,
        List<String> extractFields
) {
}
