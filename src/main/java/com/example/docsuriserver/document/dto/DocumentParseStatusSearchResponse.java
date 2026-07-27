package com.example.docsuriserver.document.dto;

import com.example.docsuriserver.document.entity.DocumentParseJob;

import java.util.UUID;

public record DocumentParseStatusSearchResponse(
        UUID parseJobId,
        String status,
        int progressPercent,
        String currentStep,
        String errorMessage
) {
    public static DocumentParseStatusSearchResponse from(DocumentParseJob job) {
        return new DocumentParseStatusSearchResponse(
                job.getParseJobId(),
                job.getStatus().name(),
                job.getProgressPercent(),
                job.getCurrentStep() == null ? null : job.getCurrentStep().name(),
                job.getErrorMessage()
        );
    }
}
