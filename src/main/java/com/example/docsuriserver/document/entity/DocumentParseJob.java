package com.example.docsuriserver.document.entity;

import com.example.docsuriserver.common.DocumentType;
import com.example.docsuriserver.common.JobStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private Map<DocumentType, List<String>> extractFields;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private JobStatus status;

    @Column(name = "progress_percent", nullable = false)
    private int progressPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", length = 50)
    private ParseStep currentStep;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extraction_documents", columnDefinition = "jsonb")
    private List<ExtractedDocument> extractionDocuments;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected DocumentParseJob() {
    }

    public static DocumentParseJob create(UUID sessionId, String ocrLanguage, Map<DocumentType, List<String>> extractFields) {
        DocumentParseJob job = new DocumentParseJob();
        job.sessionId = sessionId;
        job.ocrLanguage = ocrLanguage;
        job.extractFields = extractFields;
        job.status = JobStatus.PENDING;
        job.progressPercent = 0;
        job.currentStep = ParseStep.QUEUED;
        job.createdAt = LocalDateTime.now();
        return job;
    }

    public void progress(ParseStep step, int percent) {
        this.status = JobStatus.PROCESSING;
        this.currentStep = step;
        this.progressPercent = percent;
    }

    public void complete(List<ExtractedDocument> results) {
        this.status = JobStatus.COMPLETED;
        this.currentStep = ParseStep.DONE;
        this.progressPercent = 100;
        this.extractionDocuments = results;
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

    public boolean isActive() {
        return this.status == JobStatus.PENDING || this.status == JobStatus.PROCESSING;
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

    public Map<DocumentType, List<String>> getExtractFields() {
        return extractFields;
    }

    public JobStatus getStatus() {
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

    public String getErrorMessage() {
        return errorMessage;
    }
}
