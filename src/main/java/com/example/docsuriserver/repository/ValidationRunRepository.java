package com.example.docsuriserver.repository;

import com.example.docsuriserver.domain.ValidationRun;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValidationRunRepository extends JpaRepository<ValidationRun, UUID> {

    List<ValidationRun> findBySession_SessionId(UUID sessionId);
}
