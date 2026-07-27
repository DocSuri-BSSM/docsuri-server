package com.example.docsuriserver.repository;

import com.example.docsuriserver.domain.CorrectionRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorrectionRequestRepository extends JpaRepository<CorrectionRequest, UUID> {

    List<CorrectionRequest> findBySession_SessionId(UUID sessionId);

    List<CorrectionRequest> findByValidationRun_ValidationRunId(UUID validationRunId);
}
