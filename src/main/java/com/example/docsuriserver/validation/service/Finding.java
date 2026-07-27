package com.example.docsuriserver.validation.service;

import com.example.docsuriserver.common.IssueStatus;
import com.example.docsuriserver.common.ValidationRule;
import com.example.docsuriserver.validation.entity.ComparisonCard;

import java.util.List;

/**
 * Java 결정론 엔진이 판정을 끝낸 사실 목록. status/comparisons는 여기서 확정되며,
 * LLM은 이 값을 절대 바꾸지 않고 title/subtitle/cause/riskWarning 서술만 덧붙인다.
 */
public record Finding(
        ValidationRule rule,
        IssueStatus status,
        List<FindingValue> values,
        Double diffPercent,
        Double tolerancePercent,
        List<ComparisonCard> comparisons
) {
    public record FindingValue(String docType, String value, String unit) {
    }
}
