package com.example.docsuriserver.validation.dto;

import com.example.docsuriserver.validation.entity.ComparisonCard;
import com.example.docsuriserver.validation.entity.ValidationIssue;
import com.example.docsuriserver.validation.entity.ValidationRun;

import java.util.List;
import java.util.UUID;

public record ValidationResultSearchResponse(
        UUID validationRunId,
        String status,
        String overallSignal,
        int totalChecked,
        int normalCount,
        int warningCount,
        int errorCount,
        List<IssueItem> issues,
        String disclaimer,
        String errorMessage
) {

    public record IssueItem(
            String rule,
            String status,
            String title,
            String subtitle,
            List<ComparisonItem> comparisons,
            String cause,
            String riskWarning
    ) {
    }

    public record ComparisonItem(
            String docType,
            String docDisplayName,
            String fieldName,
            String value,
            boolean isMismatch,
            boolean isMissing
    ) {
    }

    public static ValidationResultSearchResponse from(ValidationRun run) {
        List<ValidationIssue> issues = run.getIssues() == null ? List.of() : run.getIssues();
        return new ValidationResultSearchResponse(
                run.getValidationRunId(),
                run.getStatus().name(),
                run.getOverallSignal() == null ? null : run.getOverallSignal().name(),
                run.getTotalChecked(),
                run.getNormalCount(),
                run.getWarningCount(),
                run.getErrorCount(),
                issues.stream().map(ValidationResultSearchResponse::toIssueItem).toList(),
                run.getDisclaimer(),
                run.getErrorMessage()
        );
    }

    private static IssueItem toIssueItem(ValidationIssue issue) {
        List<ComparisonItem> comparisons = issue.comparisons().stream()
                .map(ValidationResultSearchResponse::toComparisonItem)
                .toList();
        return new IssueItem(
                issue.rule().name(), issue.status().name(), issue.title(), issue.subtitle(),
                comparisons, issue.cause(), issue.riskWarning());
    }

    private static ComparisonItem toComparisonItem(ComparisonCard card) {
        return new ComparisonItem(
                card.docType().name(), card.docDisplayName(), card.fieldName(),
                card.value(), card.isMismatch(), card.isMissing());
    }
}
