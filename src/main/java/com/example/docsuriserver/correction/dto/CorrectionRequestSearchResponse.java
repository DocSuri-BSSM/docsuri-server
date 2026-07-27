package com.example.docsuriserver.correction.dto;

import com.example.docsuriserver.correction.entity.CorrectionRequest;
import com.example.docsuriserver.correction.entity.CorrectionVariable;

import java.util.List;
import java.util.UUID;

public record CorrectionRequestSearchResponse(
        UUID correctionRequestId,
        String title,
        String content,
        List<VariableItem> variables,
        String status
) {

    public record VariableItem(String variableKey, String label, String value, boolean required) {
    }

    public static CorrectionRequestSearchResponse from(CorrectionRequest r) {
        List<CorrectionVariable> variables = r.getVariables() == null ? List.of() : r.getVariables();
        List<VariableItem> items = variables.stream()
                .map(v -> new VariableItem(v.variableKey(), v.label(), v.value(), v.required()))
                .toList();
        return new CorrectionRequestSearchResponse(
                r.getCorrectionRequestId(), r.getTitle(), r.getContent(), items, r.getStatus().name());
    }
}
