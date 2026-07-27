package com.example.docsuriserver.document.repository;

import com.example.docsuriserver.document.entity.DocumentSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentSessionRepository extends JpaRepository<DocumentSession, UUID> {
}
