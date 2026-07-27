package com.example.docsuriserver.repository;

import com.example.docsuriserver.domain.DocumentSession;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentSessionRepository extends JpaRepository<DocumentSession, UUID> {
}
