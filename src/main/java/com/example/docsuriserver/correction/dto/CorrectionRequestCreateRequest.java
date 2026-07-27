package com.example.docsuriserver.correction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CorrectionRequestCreateRequest(
        @NotNull UUID validationRunId,
        @NotNull @Pattern(regexp = "KO|EN") String outputLanguage,
        String additionalInstruction
) {
}
