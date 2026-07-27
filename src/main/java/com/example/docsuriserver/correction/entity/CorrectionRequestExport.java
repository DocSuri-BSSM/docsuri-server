package com.example.docsuriserver.correction.entity;

import com.example.docsuriserver.common.ExportFormat;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "correction_request_exports")
public class CorrectionRequestExport {

    @Id
    @GeneratedValue
    @Column(name = "export_id")
    private UUID exportId;

    @Column(name = "correction_request_id", nullable = false)
    private UUID correctionRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 10)
    private ExportFormat format;

    @Column(name = "include_validation_report", nullable = false)
    private boolean includeValidationReport;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_key", nullable = false, columnDefinition = "text")
    private String fileKey;

    @Column(name = "download_url", nullable = false, columnDefinition = "text")
    private String downloadUrl;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected CorrectionRequestExport() {
    }

    public static CorrectionRequestExport create(UUID correctionRequestId, ExportFormat format,
                                                 boolean includeValidationReport, String fileName,
                                                 String fileKey, String downloadUrl, LocalDateTime expiresAt) {
        CorrectionRequestExport export = new CorrectionRequestExport();
        export.correctionRequestId = correctionRequestId;
        export.format = format;
        export.includeValidationReport = includeValidationReport;
        export.fileName = fileName;
        export.fileKey = fileKey;
        export.downloadUrl = downloadUrl;
        export.expiresAt = expiresAt;
        export.createdAt = LocalDateTime.now();
        return export;
    }

    public UUID getExportId() {
        return exportId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
