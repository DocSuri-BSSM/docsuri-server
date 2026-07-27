package com.example.docsuriserver.document.dto;

import com.example.docsuriserver.common.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

public record DocumentParseStartRequest(
        @NotBlank String ocrLanguage,
        @NotEmpty Map<DocumentType, List<String>> extractFields
) {
}
