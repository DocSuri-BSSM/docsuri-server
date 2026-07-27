package com.example.docsuriserver.document.repository;

import com.example.docsuriserver.common.JobStatus;
import com.example.docsuriserver.document.entity.DocumentParseJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentParseJobRepository extends JpaRepository<DocumentParseJob, UUID> {

    /** 한 세션에 재파싱이 있을 수 있으므로 가장 최근 job 1건을 조회 */
    Optional<DocumentParseJob> findFirstBySessionIdOrderByCreatedAtDesc(UUID sessionId);

    boolean existsBySessionIdAndStatusIn(UUID sessionId, List<JobStatus> statuses);

    List<DocumentParseJob> findAllByStatusIn(List<JobStatus> statuses);
}
