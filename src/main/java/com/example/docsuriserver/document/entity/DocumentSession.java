package com.example.docsuriserver.document.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_sessions")
public class DocumentSession {

    @Id
    @GeneratedValue
    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected DocumentSession() {
    }

    public static DocumentSession create() {
        DocumentSession session = new DocumentSession();
        session.createdAt = LocalDateTime.now();
        return session;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
