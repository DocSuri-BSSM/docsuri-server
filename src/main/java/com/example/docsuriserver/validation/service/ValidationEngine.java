package com.example.docsuriserver.validation.service;

import com.example.docsuriserver.common.DocumentType;
import com.example.docsuriserver.common.IssueStatus;
import com.example.docsuriserver.common.ValidationRule;
import com.example.docsuriserver.document.entity.ExtractedDocument;
import com.example.docsuriserver.document.entity.ExtractedField;
import com.example.docsuriserver.validation.entity.ComparisonCard;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 03-FEATURES.md 3.3절의 판정 규칙을 그대로 구현한다. 산술/숫자 비교는 전부 BigDecimal로 수행하며,
 * LLM은 절대 관여하지 않는다 — 이 클래스의 출력(Finding.status/comparisons)이 최종 판정이다.
 */
@Component
public class ValidationEngine {

    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.01");

    private enum ComparisonType { STRING_EXACT, STRING_NORMALIZED, NUMERIC_EXACT, NUMERIC_TOLERANCE, ARITHMETIC }

    private record RuleSpec(int tier, List<String> fieldKeys, ComparisonType type) {
    }

    private static final Map<ValidationRule, RuleSpec> RULE_SPECS = Map.ofEntries(
            Map.entry(ValidationRule.DESCRIPTION_MATCH, new RuleSpec(1, List.of("PRODUCT_NAME"), ComparisonType.STRING_NORMALIZED)),
            Map.entry(ValidationRule.GROSS_WEIGHT_MATCH, new RuleSpec(1, List.of("GROSS_WEIGHT"), ComparisonType.NUMERIC_TOLERANCE)),
            Map.entry(ValidationRule.PACKAGE_QTY_MATCH, new RuleSpec(1, List.of("PACKAGE_QTY"), ComparisonType.NUMERIC_EXACT)),
            Map.entry(ValidationRule.PARTY_NAME_MATCH, new RuleSpec(1, List.of("SHIPPER_NAME", "CONSIGNEE_NAME"), ComparisonType.STRING_NORMALIZED)),
            Map.entry(ValidationRule.AMOUNT_CALCULATION, new RuleSpec(1, List.of("QUANTITY", "UNIT_PRICE", "TOTAL_AMOUNT"), ComparisonType.ARITHMETIC)),
            Map.entry(ValidationRule.INVOICE_NO_MATCH, new RuleSpec(2, List.of("INVOICE_NO"), ComparisonType.STRING_EXACT)),
            Map.entry(ValidationRule.VESSEL_VOYAGE_MATCH, new RuleSpec(2, List.of("VESSEL_VOYAGE"), ComparisonType.STRING_NORMALIZED)),
            Map.entry(ValidationRule.PORT_MATCH, new RuleSpec(2, List.of("PORT_OF_LOADING", "PORT_OF_DISCHARGE"), ComparisonType.STRING_NORMALIZED)),
            Map.entry(ValidationRule.SHIPPING_MARKS_MATCH, new RuleSpec(2, List.of("SHIPPING_MARKS"), ComparisonType.STRING_NORMALIZED)),
            Map.entry(ValidationRule.ADDRESS_MATCH, new RuleSpec(2, List.of("SHIPPER_ADDRESS", "CONSIGNEE_ADDRESS"), ComparisonType.STRING_NORMALIZED))
    );

    public List<Finding> evaluate(List<ExtractedDocument> documents, List<ValidationRule> requestedRules,
                                   BigDecimal weightTolerancePercent) {
        List<Finding> findings = new ArrayList<>();
        for (ValidationRule rule : requestedRules) {
            RuleSpec spec = RULE_SPECS.get(rule);
            if (spec == null) {
                continue;
            }
            Finding finding = switch (spec.type()) {
                case ARITHMETIC -> evaluateAmountCalculation(documents);
                case NUMERIC_TOLERANCE -> evaluateGrossWeight(rule, spec, documents, weightTolerancePercent);
                case NUMERIC_EXACT -> evaluateNumericExact(rule, spec, documents);
                case STRING_EXACT -> evaluateStringRule(rule, spec, documents, false);
                case STRING_NORMALIZED -> evaluateStringRule(rule, spec, documents, true);
            };
            if (finding != null) {
                findings.add(finding);
            }
        }
        return findings;
    }

    // ---- STRING_EXACT / STRING_NORMALIZED (단일/복수 필드 공용) ----

    private Finding evaluateStringRule(ValidationRule rule, RuleSpec spec, List<ExtractedDocument> documents, boolean normalize) {
        List<Finding.FindingValue> allValues = new ArrayList<>();
        List<ComparisonCard> allComparisons = new ArrayList<>();
        boolean anyIssue = false;
        int comparableFieldCount = 0;

        for (String fieldKey : spec.fieldKeys()) {
            FieldEvalResult r = evaluateFieldKey(fieldKey, documents, normalize);
            if (r.attemptedCount() < 2) {
                continue;
            }
            comparableFieldCount++;
            allValues.addAll(r.values());
            allComparisons.addAll(r.comparisons());
            if (r.issue()) {
                anyIssue = true;
            }
        }

        if (comparableFieldCount == 0) {
            return null;
        }

        IssueStatus status = anyIssue ? tierDefault(spec.tier()) : IssueStatus.NORMAL;
        return new Finding(rule, status, allValues, null, null, allComparisons);
    }

    private record FieldEvalResult(List<Finding.FindingValue> values, List<ComparisonCard> comparisons,
                                    boolean issue, int attemptedCount) {
    }

    private FieldEvalResult evaluateFieldKey(String fieldKey, List<ExtractedDocument> documents, boolean normalize) {
        List<Finding.FindingValue> values = new ArrayList<>();
        List<ComparisonCard> comparisons = new ArrayList<>();
        List<Integer> comparableIndexes = new ArrayList<>();
        List<String> comparableNormalized = new ArrayList<>();
        boolean anyMissing = false;

        for (ExtractedDocument doc : documents) {
            Optional<ExtractedField> field = findField(doc, fieldKey);
            if (field.isEmpty()) {
                continue;
            }
            ExtractedField f = field.get();
            boolean missing = f.value() == null || f.value().isBlank();
            values.add(new Finding.FindingValue(doc.documentType().name(), f.value(), f.unit()));
            if (missing) {
                anyMissing = true;
                comparisons.add(new ComparisonCard(doc.documentType(), displayNameOf(doc.documentType()), fieldKey, "", false, true));
            } else {
                comparableIndexes.add(comparisons.size());
                comparableNormalized.add(normalize ? normalizeText(f.value()) : f.value());
                comparisons.add(new ComparisonCard(doc.documentType(), displayNameOf(doc.documentType()), fieldKey, f.value(), false, false));
            }
        }

        int attemptedCount = values.size();
        boolean mismatch = false;
        if (comparableNormalized.size() >= 2) {
            String reference = comparableNormalized.get(0);
            for (int i = 0; i < comparableNormalized.size(); i++) {
                if (!comparableNormalized.get(i).equals(reference)) {
                    mismatch = true;
                    int cardIndex = comparableIndexes.get(i);
                    comparisons.set(cardIndex, withMismatch(comparisons.get(cardIndex)));
                }
            }
        }

        // 필수 항목 누락은 Tier와 무관하게 issue로 취급한다 (03-FEATURES.md 3.1절)
        return new FieldEvalResult(values, comparisons, mismatch || anyMissing, attemptedCount);
    }

    // ---- NUMERIC_EXACT (수량 등 완전 일치) ----

    private Finding evaluateNumericExact(ValidationRule rule, RuleSpec spec, List<ExtractedDocument> documents) {
        String fieldKey = spec.fieldKeys().get(0);
        List<Finding.FindingValue> values = new ArrayList<>();
        List<ComparisonCard> comparisons = new ArrayList<>();
        List<BigDecimal> numbers = new ArrayList<>();
        boolean anyMissing = false;

        for (ExtractedDocument doc : documents) {
            Optional<ExtractedField> field = findField(doc, fieldKey);
            if (field.isEmpty()) {
                continue;
            }
            ExtractedField f = field.get();
            values.add(new Finding.FindingValue(doc.documentType().name(), f.value(), f.unit()));
            if (f.valueNumber() == null) {
                anyMissing = true;
                comparisons.add(new ComparisonCard(doc.documentType(), displayNameOf(doc.documentType()), fieldKey, "", false, true));
            } else {
                numbers.add(f.valueNumber());
                comparisons.add(new ComparisonCard(doc.documentType(), displayNameOf(doc.documentType()), fieldKey, f.value(), false, false));
            }
        }

        if (values.size() < 2) {
            return null;
        }

        boolean mismatch = false;
        if (numbers.size() >= 2) {
            BigDecimal reference = numbers.get(0);
            int numberIdx = 0;
            for (int i = 0; i < comparisons.size(); i++) {
                if (comparisons.get(i).isMissing()) {
                    continue;
                }
                BigDecimal n = numbers.get(numberIdx++);
                if (n.compareTo(reference) != 0) {
                    mismatch = true;
                    comparisons.set(i, withMismatch(comparisons.get(i)));
                }
            }
        }

        IssueStatus status = (mismatch || anyMissing) ? tierDefault(spec.tier()) : IssueStatus.NORMAL;
        return new Finding(rule, status, values, null, null, comparisons);
    }

    // ---- NUMERIC_TOLERANCE (총중량, 단위 환산 + 허용오차) ----

    private Finding evaluateGrossWeight(ValidationRule rule, RuleSpec spec, List<ExtractedDocument> documents,
                                         BigDecimal tolerancePercent) {
        String fieldKey = spec.fieldKeys().get(0);
        List<Finding.FindingValue> values = new ArrayList<>();
        List<ComparisonCard> comparisons = new ArrayList<>();
        List<BigDecimal> kgValues = new ArrayList<>();
        boolean unitConversionFailed = false;
        boolean anyMissing = false;

        for (ExtractedDocument doc : documents) {
            Optional<ExtractedField> field = findField(doc, fieldKey);
            if (field.isEmpty()) {
                continue;
            }
            ExtractedField f = field.get();
            values.add(new Finding.FindingValue(doc.documentType().name(), f.value(), f.unit()));
            if (f.valueNumber() == null) {
                anyMissing = true;
                comparisons.add(new ComparisonCard(doc.documentType(), displayNameOf(doc.documentType()), fieldKey, "", false, true));
                continue;
            }
            BigDecimal kg = toKg(f.valueNumber(), f.unit());
            comparisons.add(new ComparisonCard(doc.documentType(), displayNameOf(doc.documentType()), fieldKey, f.value(), false, false));
            if (kg == null) {
                unitConversionFailed = true;
            } else {
                kgValues.add(kg);
            }
        }

        if (values.size() < 2) {
            return null;
        }

        if (anyMissing || unitConversionFailed) {
            return new Finding(rule, IssueStatus.ERROR, values, null, tolerancePercent.doubleValue(), comparisons);
        }

        BigDecimal max = kgValues.stream().max(Comparator.naturalOrder()).orElseThrow();
        BigDecimal min = kgValues.stream().min(Comparator.naturalOrder()).orElseThrow();
        double diffPercent = max.compareTo(BigDecimal.ZERO) == 0
                ? 0.0
                : max.subtract(min).abs()
                        .divide(max, 10, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .doubleValue();

        boolean withinTolerance = BigDecimal.valueOf(diffPercent).compareTo(tolerancePercent) <= 0;
        if (!withinTolerance) {
            for (int i = 0; i < comparisons.size(); i++) {
                if (kgValues.get(i).compareTo(max) != 0) {
                    comparisons.set(i, withMismatch(comparisons.get(i)));
                }
            }
        }

        IssueStatus status = withinTolerance ? IssueStatus.NORMAL : IssueStatus.ERROR;
        return new Finding(rule, status, values, diffPercent, tolerancePercent.doubleValue(), comparisons);
    }

    private BigDecimal toKg(BigDecimal value, String unit) {
        if (unit == null || unit.isBlank()) {
            return value;
        }
        return switch (unit.trim().toUpperCase(Locale.ROOT)) {
            case "KG" -> value;
            case "T", "TON" -> value.multiply(new BigDecimal("1000"));
            case "LB" -> value.multiply(new BigDecimal("0.453592"));
            default -> null;
        };
    }

    // ---- ARITHMETIC (수량 × 단가 = 총액 검산) ----

    private Finding evaluateAmountCalculation(List<ExtractedDocument> documents) {
        for (ExtractedDocument doc : documents) {
            Optional<ExtractedField> qty = findField(doc, "QUANTITY");
            Optional<ExtractedField> unitPrice = findField(doc, "UNIT_PRICE");
            Optional<ExtractedField> total = findField(doc, "TOTAL_AMOUNT");
            if (qty.isEmpty() || unitPrice.isEmpty() || total.isEmpty()) {
                continue;
            }
            if (qty.get().valueNumber() == null || unitPrice.get().valueNumber() == null || total.get().valueNumber() == null) {
                continue;
            }

            BigDecimal expected = qty.get().valueNumber().multiply(unitPrice.get().valueNumber());
            BigDecimal actual = total.get().valueNumber();
            boolean matches = expected.subtract(actual).abs().compareTo(AMOUNT_TOLERANCE) <= 0;

            List<Finding.FindingValue> values = List.of(
                    new Finding.FindingValue(doc.documentType().name(), qty.get().value(), qty.get().unit()),
                    new Finding.FindingValue(doc.documentType().name(), unitPrice.get().value(), unitPrice.get().unit()),
                    new Finding.FindingValue(doc.documentType().name(), total.get().value(), total.get().unit()));

            List<ComparisonCard> comparisons = List.of(
                    new ComparisonCard(doc.documentType(), displayNameOf(doc.documentType()), "QUANTITY", qty.get().value(), false, false),
                    new ComparisonCard(doc.documentType(), displayNameOf(doc.documentType()), "UNIT_PRICE", unitPrice.get().value(), false, false),
                    new ComparisonCard(doc.documentType(), displayNameOf(doc.documentType()), "TOTAL_AMOUNT", total.get().value(), !matches, false));

            return new Finding(ValidationRule.AMOUNT_CALCULATION,
                    matches ? IssueStatus.NORMAL : IssueStatus.ERROR, values, null, null, comparisons);
        }
        return null; // QUANTITY/UNIT_PRICE/TOTAL_AMOUNT를 모두 가진 서류가 없으면 이 규칙은 건너뛴다
    }

    // ---- 공용 유틸 ----

    private Optional<ExtractedField> findField(ExtractedDocument doc, String fieldKey) {
        return doc.fields().stream().filter(f -> f.fieldKey().equalsIgnoreCase(fieldKey)).findFirst();
    }

    private IssueStatus tierDefault(int tier) {
        return tier == 1 ? IssueStatus.ERROR : IssueStatus.WARNING;
    }

    private ComparisonCard withMismatch(ComparisonCard card) {
        return new ComparisonCard(card.docType(), card.docDisplayName(), card.fieldName(), card.value(), true, card.isMissing());
    }

    private String normalizeText(String value) {
        return value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9 ]", "");
    }

    private String displayNameOf(DocumentType type) {
        return switch (type) {
            case INVOICE -> "상업송장";
            case BILL_OF_LADING -> "선하증권";
            case PACKING_LIST -> "포장명세서";
        };
    }
}
