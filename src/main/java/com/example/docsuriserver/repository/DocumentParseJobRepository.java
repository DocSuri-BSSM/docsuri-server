package com.example.docsuriserver.repository;

import com.example.docsuriserver.domain.DocumentParseJob;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentParseJobRepository extends JpaRepository<DocumentParseJob, UUID> {

    List<DocumentParseJob> findBySession_SessionId(UUID sessionId);

    List<DocumentParseJob> findBySession_SessionIdAndStatus(UUID sessionId, String status);
}
