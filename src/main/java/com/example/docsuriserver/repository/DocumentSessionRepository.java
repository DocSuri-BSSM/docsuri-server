package com.example.docsuriserver.repository;

import com.example.docsuriserver.entity.DocumentSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentSessionRepository extends JpaRepository<DocumentSession, UUID> {
}
