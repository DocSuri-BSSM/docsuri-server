package com.example.docsuriserver.entity;

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

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Document() {
    }

    public static Document create(UUID sessionId, DocumentType type, String originalFileName, String fileUrl) {
        Document document = new Document();
        document.sessionId = sessionId;
        document.documentType = type;
        document.originalFileName = originalFileName;
        document.fileUrl = fileUrl;
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
}
