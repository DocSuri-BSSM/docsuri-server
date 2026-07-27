package com.example.docsuriserver.correction.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CorrectionRequestUpdateRequest(
        String title,
        String content,
        List<VariableUpdate> variables
) {
    public record VariableUpdate(@NotBlank String variableKey, String value) {
    }
}
