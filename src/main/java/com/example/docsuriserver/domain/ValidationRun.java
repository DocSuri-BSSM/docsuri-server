package com.example.docsuriserver.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 문서 검증 실행 결과(규칙, 신호등, 이슈 목록 등).
 */
@Entity
@Table(name = "validation_runs")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ValidationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "validation_run_id", updatable = false, nullable = false)
    private UUID validationRunId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private DocumentSession session;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rules", columnDefinition = "jsonb", nullable = false)
    private JsonNode rules;

    @Column(name = "weight_tolerance_percent", precision = 7, scale = 4)
    private BigDecimal weightTolerancePercent;

    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "overall_signal", length = 20)
    private String overallSignal;

    @Column(name = "normal_count", nullable = false)
    private Integer normalCount;

    @Column(name = "warning_count", nullable = false)
    private Integer warningCount;

    @Column(name = "error_count", nullable = false)
    private Integer errorCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "issues", columnDefinition = "jsonb")
    private JsonNode issues;

    @Column(name = "disclaimer", columnDefinition = "text")
    private String disclaimer;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
