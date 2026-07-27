package com.example.docsuriserver.validation.service;

import com.example.docsuriserver.common.DocumentType;
import com.example.docsuriserver.common.IssueStatus;
import com.example.docsuriserver.common.ValidationRule;
import com.example.docsuriserver.document.entity.ExtractedDocument;
import com.example.docsuriserver.document.entity.ExtractedField;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ValidationEngine은 GeminiClient에 의존하지 않는 순수 Java 로직이므로,
 * 여기서 통과하면 "LLM을 스텁으로 바꿔도 등급/카운트/신호등이 동일하다"는 것이
 * 구조적으로 보장된다 (05-TASKS.md T6 완료조건).
 */
class ValidationEngineTest {

    private final ValidationEngine engine = new ValidationEngine();

    private ExtractedDocument doc(DocumentType type, ExtractedField... fields) {
        return new ExtractedDocument(UUID.randomUUID(), type, List.of(fields));
    }

    private ExtractedField field(String key, String value, String number, String unit) {
        return new ExtractedField(key, key, value, number == null ? null : new BigDecimal(number), unit, new BigDecimal("0.97"), 1);
    }

    @Test
    void grossWeightOutsideToleranceIsError() {
        List<ExtractedDocument> documents = List.of(
                doc(DocumentType.INVOICE, field("GROSS_WEIGHT", "12500", "12500", "KG")),
                doc(DocumentType.BILL_OF_LADING, field("GROSS_WEIGHT", "11800", "11800", "KG")));

        List<Finding> findings = engine.evaluate(documents, List.of(ValidationRule.GROSS_WEIGHT_MATCH), new BigDecimal("0.5"));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).status()).isEqualTo(IssueStatus.ERROR);
    }

    @Test
    void grossWeightWithinToleranceIsNormal() {
        List<ExtractedDocument> documents = List.of(
                doc(DocumentType.INVOICE, field("GROSS_WEIGHT", "12500", "12500", "KG")),
                doc(DocumentType.BILL_OF_LADING, field("GROSS_WEIGHT", "12499", "12499", "KG")));

        List<Finding> findings = engine.evaluate(documents, List.of(ValidationRule.GROSS_WEIGHT_MATCH), new BigDecimal("0.5"));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).status()).isEqualTo(IssueStatus.NORMAL);
    }

    @Test
    void amountCalculationMismatchIsErrorUsingBigDecimal() {
        List<ExtractedDocument> documents = List.of(
                doc(DocumentType.INVOICE,
                        field("QUANTITY", "500", "500", "CTN"),
                        field("UNIT_PRICE", "39.00", "39.00", "USD"),
                        field("TOTAL_AMOUNT", "49750", "49750", "USD")));

        List<Finding> findings = engine.evaluate(documents, List.of(ValidationRule.AMOUNT_CALCULATION), BigDecimal.ZERO);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).status()).isEqualTo(IssueStatus.ERROR);
    }

    @Test
    void amountCalculationMatchIsNormal() {
        List<ExtractedDocument> documents = List.of(
                doc(DocumentType.INVOICE,
                        field("QUANTITY", "500", "500", "CTN"),
                        field("UNIT_PRICE", "39.00", "39.00", "USD"),
                        field("TOTAL_AMOUNT", "19500.00", "19500.00", "USD")));

        List<Finding> findings = engine.evaluate(documents, List.of(ValidationRule.AMOUNT_CALCULATION), BigDecimal.ZERO);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).status()).isEqualTo(IssueStatus.NORMAL);
    }

    @Test
    void ruleWithOnlyOneComparableDocumentIsSkipped() {
        List<ExtractedDocument> documents = List.of(
                doc(DocumentType.INVOICE, field("GROSS_WEIGHT", "12500", "12500", "KG")));

        List<Finding> findings = engine.evaluate(documents, List.of(ValidationRule.GROSS_WEIGHT_MATCH), new BigDecimal("0.5"));

        assertThat(findings).isEmpty();
    }

    @Test
    void countInvariantHoldsAcrossMixedResults() {
        List<ExtractedDocument> documents = List.of(
                doc(DocumentType.INVOICE,
                        field("GROSS_WEIGHT", "12500", "12500", "KG"),
                        field("PACKAGE_QTY", "100", "100", "CTN"),
                        field("PRODUCT_NAME", "FROZEN MACKEREL", null, null)),
                doc(DocumentType.BILL_OF_LADING,
                        field("GROSS_WEIGHT", "11800", "11800", "KG"),
                        field("PACKAGE_QTY", "100", "100", "CTN"),
                        field("PRODUCT_NAME", "FROZEN MACKEREL", null, null)));

        List<ValidationRule> rules = List.of(
                ValidationRule.GROSS_WEIGHT_MATCH, ValidationRule.PACKAGE_QTY_MATCH, ValidationRule.DESCRIPTION_MATCH);
        List<Finding> findings = engine.evaluate(documents, rules, new BigDecimal("0.5"));

        long normal = findings.stream().filter(f -> f.status() == IssueStatus.NORMAL).count();
        long warning = findings.stream().filter(f -> f.status() == IssueStatus.WARNING).count();
        long error = findings.stream().filter(f -> f.status() == IssueStatus.ERROR).count();

        assertThat(normal + warning + error).isEqualTo(findings.size());
        assertThat(findings).hasSize(3);
    }
}
