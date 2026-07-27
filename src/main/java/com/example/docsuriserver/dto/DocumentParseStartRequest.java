package com.example.docsuriserver.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record DocumentParseStartRequest(
        @NotBlank String ocrLanguage,
        @NotEmpty List<String> extractFields
) {
}
