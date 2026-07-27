package com.example.docsuriserver.hscode.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "hs_code_recommendations")
public class HsCodeRecommendation {

    @Id
    @GeneratedValue
    @Column(name = "recommendation_id")
    private UUID recommendationId;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_description", columnDefinition = "text")
    private String productDescription;

    @Column(name = "origin_country_code", length = 10)
    private String originCountryCode;

    @Column(name = "max_candidates", nullable = false)
    private int maxCandidates;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "candidates", nullable = false, columnDefinition = "jsonb")
    private List<HsCodeCandidate> candidates;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected HsCodeRecommendation() {
    }

    public static HsCodeRecommendation create(String productName, String productDescription,
                                              String originCountryCode, int maxCandidates,
                                              List<HsCodeCandidate> candidates) {
        HsCodeRecommendation r = new HsCodeRecommendation();
        r.productName = productName;
        r.productDescription = productDescription;
        r.originCountryCode = originCountryCode;
        r.maxCandidates = maxCandidates;
        r.candidates = candidates;
        r.createdAt = LocalDateTime.now();
        return r;
    }

    public UUID getRecommendationId() {
        return recommendationId;
    }

    public List<HsCodeCandidate> getCandidates() {
        return candidates;
    }
}
