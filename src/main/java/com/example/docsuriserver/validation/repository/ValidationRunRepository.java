package com.example.docsuriserver.validation.repository;

import com.example.docsuriserver.common.JobStatus;
import com.example.docsuriserver.validation.entity.ValidationRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ValidationRunRepository extends JpaRepository<ValidationRun, UUID> {

    Optional<ValidationRun> findFirstBySessionIdOrderByCreatedAtDesc(UUID sessionId);

    boolean existsBySessionIdAndStatusIn(UUID sessionId, List<JobStatus> statuses);

    List<ValidationRun> findAllByStatusIn(List<JobStatus> statuses);
}
