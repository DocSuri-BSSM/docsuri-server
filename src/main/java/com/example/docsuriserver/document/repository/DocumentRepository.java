package com.example.docsuriserver.document.repository;

import com.example.docsuriserver.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findAllBySessionId(UUID sessionId);
}
