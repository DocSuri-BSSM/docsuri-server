package com.example.docsuriserver.document.entity;

import com.example.docsuriserver.common.DocumentType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue
    @Column(name = "document_id")
    private UUID documentId;

    // MVP 단계에서는 연관관계 매핑 대신 FK 값만 보관 (조회 쿼리가 단순해짐)
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private DocumentType documentType;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "file_url", nullable = false, columnDefinition = "text")
    private String fileUrl;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Document() {
    }

    public static Document create(UUID sessionId, DocumentType type, String originalFileName, String fileUrl,
                                   String contentType, long fileSize) {
        Document document = new Document();
        document.sessionId = sessionId;
        document.documentType = type;
        document.originalFileName = originalFileName;
        document.fileUrl = fileUrl;
        document.contentType = contentType;
        document.fileSize = fileSize;
        document.createdAt = LocalDateTime.now();
        return document;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }
}
