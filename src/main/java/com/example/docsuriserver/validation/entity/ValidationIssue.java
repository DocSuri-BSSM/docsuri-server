package com.example.docsuriserver.validation.entity;

import com.example.docsuriserver.common.IssueStatus;
import com.example.docsuriserver.common.ValidationRule;

import java.util.List;

public record ValidationIssue(
        ValidationRule rule,
        IssueStatus status,
        String title,
        String subtitle,
        List<ComparisonCard> comparisons,
        String cause,
        String riskWarning
) {
}
