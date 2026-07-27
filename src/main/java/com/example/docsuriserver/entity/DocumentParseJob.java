package com.example.docsuriserver.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "document_parse_jobs")
public class DocumentParseJob {

    @Id
    @GeneratedValue
    @Column(name = "parse_job_id")
    private UUID parseJobId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "ocr_language", length = 50)
    private String ocrLanguage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extract_fields", nullable = false, columnDefinition = "jsonb")
    private List<String> extractFields;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ParseStatus status;

    @Column(name = "progress_percent", nullable = false)
    private int progressPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", length = 50)
    private ParseStep currentStep;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extraction_documents", columnDefinition = "jsonb")
    private List<ExtractedDocument> extractionDocuments;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected DocumentParseJob() {
    }

    public static DocumentParseJob create(UUID sessionId, String ocrLanguage, List<String> extractFields) {
        DocumentParseJob job = new DocumentParseJob();
        job.sessionId = sessionId;
        job.ocrLanguage = ocrLanguage;
        job.extractFields = extractFields;
        job.status = ParseStatus.PENDING;
        job.progressPercent = 0;
        job.currentStep = ParseStep.QUEUED;
        job.createdAt = LocalDateTime.now();
        return job;
    }

    public void progress(ParseStep step, int percent) {
        this.status = ParseStatus.PROCESSING;
        this.currentStep = step;
        this.progressPercent = percent;
    }

    public void complete(List<ExtractedDocument> results) {
        this.status = ParseStatus.COMPLETED;
        this.currentStep = ParseStep.DONE;
        this.progressPercent = 100;
        this.extractionDocuments = results;
        this.completedAt = LocalDateTime.now();
    }

    public void fail() {
        this.status = ParseStatus.FAILED;
        this.completedAt = LocalDateTime.now();
    }

    public boolean isCompleted() {
        return this.status == ParseStatus.COMPLETED;
    }

    public UUID getParseJobId() {
        return parseJobId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public String getOcrLanguage() {
        return ocrLanguage;
    }

    public List<String> getExtractFields() {
        return extractFields;
    }

    public ParseStatus getStatus() {
        return status;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public ParseStep getCurrentStep() {
        return currentStep;
    }

    public List<ExtractedDocument> getExtractionDocuments() {
        return extractionDocuments;
    }
}
