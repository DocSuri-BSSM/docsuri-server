package com.example.docsuriserver.validation.service;

import com.example.docsuriserver.common.JobStatus;
import com.example.docsuriserver.validation.entity.ValidationRun;
import com.example.docsuriserver.validation.repository.ValidationRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 스레드풀이 인메모리라 재시작 시 유실되므로, 기동 시점에 PENDING/PROCESSING 으로 남은 검증 작업을 정리한다.
 */
@Component
public class ValidationRunCleanupListener {

    private static final Logger log = LoggerFactory.getLogger(ValidationRunCleanupListener.class);
    private static final List<JobStatus> GHOST_STATUSES = List.of(JobStatus.PENDING, JobStatus.PROCESSING);

    private final ValidationRunRepository runRepository;

    public ValidationRunCleanupListener(ValidationRunRepository runRepository) {
        this.runRepository = runRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void cleanupGhostRuns() {
        List<ValidationRun> ghosts = runRepository.findAllByStatusIn(GHOST_STATUSES);
        if (ghosts.isEmpty()) {
            return;
        }
        ghosts.forEach(run -> run.fail("서버 재시작으로 작업이 중단되었습니다."));
        runRepository.saveAll(ghosts);
        log.info("유령 검증 작업 {}건을 FAILED로 정리했습니다.", ghosts.size());
    }
}
