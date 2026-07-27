package com.example.docsuriserver.correction.repository;

import com.example.docsuriserver.correction.entity.CorrectionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CorrectionRequestRepository extends JpaRepository<CorrectionRequest, UUID> {
}
