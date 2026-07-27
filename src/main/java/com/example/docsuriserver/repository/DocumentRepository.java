package com.example.docsuriserver.repository;

import com.example.docsuriserver.domain.Document;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findBySession_SessionId(UUID sessionId);

    List<Document> findBySession_SessionIdAndDocumentType(UUID sessionId, String documentType);
}
