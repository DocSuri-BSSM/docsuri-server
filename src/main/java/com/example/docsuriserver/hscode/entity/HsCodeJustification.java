package com.example.docsuriserver.hscode.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "hs_code_justifications")
public class HsCodeJustification {

    @Id
    @GeneratedValue
    @Column(name = "justification_id")
    private UUID justificationId;

    @Column(name = "hs_code", nullable = false, length = 20)
    private String hsCode;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_description", columnDefinition = "text")
    private String productDescription;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_facts", columnDefinition = "jsonb")
    private List<String> additionalFacts;

    @Column(name = "output_language", nullable = false, length = 10)
    private String outputLanguage;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "legal_basis", nullable = false, columnDefinition = "jsonb")
    private List<String> legalBasis;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected HsCodeJustification() {
    }

    public static HsCodeJustification create(String hsCode, String productName, String productDescription,
                                             List<String> additionalFacts, String outputLanguage, String title,
                                             String content, List<String> legalBasis) {
        HsCodeJustification j = new HsCodeJustification();
        j.hsCode = hsCode;
        j.productName = productName;
        j.productDescription = productDescription;
        j.additionalFacts = additionalFacts;
        j.outputLanguage = outputLanguage;
        j.title = title;
        j.content = content;
        j.legalBasis = legalBasis;
        j.createdAt = LocalDateTime.now();
        return j;
    }

    public UUID getJustificationId() {
        return justificationId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public List<String> getLegalBasis() {
        return legalBasis;
    }
}
