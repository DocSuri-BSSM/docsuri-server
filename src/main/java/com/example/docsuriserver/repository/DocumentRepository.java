package com.example.docsuriserver.repository;

import com.example.docsuriserver.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findAllBySessionId(UUID sessionId);
}
