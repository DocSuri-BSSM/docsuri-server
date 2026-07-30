package com.example.docsuriserver.validation.entity;

import com.example.docsuriserver.common.JobStatus;
import com.example.docsuriserver.common.OverallSignal;
import com.example.docsuriserver.common.ValidationRule;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "validation_runs")
public class ValidationRun {

    @Id
    @GeneratedValue
    @Column(name = "validation_run_id")
    private UUID validationRunId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rules", nullable = false, columnDefinition = "jsonb")
    private List<ValidationRule> rules;

    @Column(name = "weight_tolerance_percent", nullable = false)
    private BigDecimal weightTolerancePercent;

    @Column(name = "output_language", nullable = false, length = 10)
    private String outputLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private JobStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_signal", length = 20)
    private OverallSignal overallSignal;

    @Column(name = "total_checked", nullable = false)
    private int totalChecked;

    @Column(name = "normal_count", nullable = false)
    private int normalCount;

    @Column(name = "warning_count", nullable = false)
    private int warningCount;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "issues", columnDefinition = "jsonb")
    private List<ValidationIssue> issues;

    @Column(name = "disclaimer", columnDefinition = "text")
    private String disclaimer;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected ValidationRun() {
    }

    public static ValidationRun create(UUID sessionId, List<ValidationRule> rules, BigDecimal weightTolerancePercent,
                                       String outputLanguage) {
        ValidationRun run = new ValidationRun();
        run.sessionId = sessionId;
        run.rules = rules;
        run.weightTolerancePercent = weightTolerancePercent;
        run.outputLanguage = outputLanguage;
        run.status = JobStatus.PENDING;
        run.totalChecked = 0;
        run.normalCount = 0;
        run.warningCount = 0;
        run.errorCount = 0;
        run.createdAt = LocalDateTime.now();
        return run;
    }

    public void start() {
        this.status = JobStatus.PROCESSING;
    }

    public void complete(OverallSignal overallSignal, int totalChecked, int normalCount, int warningCount,
                         int errorCount, List<ValidationIssue> issues, String disclaimer) {
        this.status = JobStatus.COMPLETED;
        this.overallSignal = overallSignal;
        this.totalChecked = totalChecked;
        this.normalCount = normalCount;
        this.warningCount = warningCount;
        this.errorCount = errorCount;
        this.issues = issues;
        this.disclaimer = disclaimer;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    public boolean isCompleted() {
        return this.status == JobStatus.COMPLETED;
    }

    public UUID getValidationRunId() {
        return validationRunId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public List<ValidationRule> getRules() {
        return rules;
    }

    public BigDecimal getWeightTolerancePercent() {
        return weightTolerancePercent;
    }

    public String getOutputLanguage() {
        return outputLanguage;
    }

    public JobStatus getStatus() {
        return status;
    }

    public OverallSignal getOverallSignal() {
        return overallSignal;
    }

    public int getTotalChecked() {
        return totalChecked;
    }

    public int getNormalCount() {
        return normalCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public List<ValidationIssue> getIssues() {
        return issues;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
