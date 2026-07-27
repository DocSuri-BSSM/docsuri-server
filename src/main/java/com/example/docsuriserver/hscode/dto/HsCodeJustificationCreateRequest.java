package com.example.docsuriserver.hscode.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record HsCodeJustificationCreateRequest(
        @NotBlank String hsCode,
        @NotBlank String productName,
        @NotBlank String productDescription,
        @NotNull List<String> additionalFacts
) {
}
