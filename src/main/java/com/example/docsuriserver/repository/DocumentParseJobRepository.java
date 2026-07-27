package com.example.docsuriserver.repository;

import com.example.docsuriserver.entity.DocumentParseJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentParseJobRepository extends JpaRepository<DocumentParseJob, UUID> {

    /** 한 세션에 재파싱이 있을 수 있으므로 가장 최근 job 1건을 조회 */
    Optional<DocumentParseJob> findFirstBySessionIdOrderByCreatedAtDesc(UUID sessionId);
}
