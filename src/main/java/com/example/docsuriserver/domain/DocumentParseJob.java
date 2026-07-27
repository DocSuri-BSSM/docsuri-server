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
 * OCR/필드추출 파싱 작업(비동기 진행 상태 포함).
 */
@Entity
@Table(name = "document_parse_jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DocumentParseJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "parse_job_id", updatable = false, nullable = false)
    private UUID parseJobId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private DocumentSession session;

    @Column(name = "ocr_language", length = 50)
    private String ocrLanguage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extract_fields", columnDefinition = "jsonb", nullable = false)
    private JsonNode extractFields;

    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "progress_percent", nullable = false)
    private Integer progressPercent;

    @Column(name = "current_step", length = 50)
    private String currentStep;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extraction_documents", columnDefinition = "jsonb")
    private JsonNode extractionDocuments;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
