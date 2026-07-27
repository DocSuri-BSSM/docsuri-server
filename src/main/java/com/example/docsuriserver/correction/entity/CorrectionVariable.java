package com.example.docsuriserver.correction.entity;

public record CorrectionVariable(
        String variableKey,
        String label,
        String value,
        boolean required
) {
}
