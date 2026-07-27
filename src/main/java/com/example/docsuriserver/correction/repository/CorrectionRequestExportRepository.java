package com.example.docsuriserver.correction.repository;

import com.example.docsuriserver.correction.entity.CorrectionRequestExport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CorrectionRequestExportRepository extends JpaRepository<CorrectionRequestExport, UUID> {
}
