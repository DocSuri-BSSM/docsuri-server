package com.example.docsuriserver.hscode.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record HsCodeRecommendationCreateRequest(
        @NotBlank String productName,
        @NotBlank String productDescription,
        String originCountryCode,
        @NotNull @Min(1) @Max(10) Integer maxCandidates,
        @NotNull @Pattern(regexp = "KO|EN") String outputLanguage
) {
}
