package com.example.docsuriserver.hscode.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record HsCodeJustificationCreateRequest(
        @NotBlank String hsCode,
        @NotBlank String productName,
        @NotBlank String productDescription,
        @NotNull List<String> additionalFacts,
        @NotNull @Pattern(regexp = "KO|EN") String outputLanguage
) {
}
