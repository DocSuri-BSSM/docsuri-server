package com.example.docsuriserver.hscode.repository;

import com.example.docsuriserver.hscode.entity.HsCodeJustification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HsCodeJustificationRepository extends JpaRepository<HsCodeJustification, UUID> {
}
