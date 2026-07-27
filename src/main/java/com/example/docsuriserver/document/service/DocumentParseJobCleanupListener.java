package com.example.docsuriserver.document.service;

import com.example.docsuriserver.common.JobStatus;
import com.example.docsuriserver.document.entity.DocumentParseJob;
import com.example.docsuriserver.document.repository.DocumentParseJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 스레드풀이 인메모리라 재시작 시 유실되므로, 기동 시점에 PENDING/PROCESSING 으로 남은 job을 정리한다.
 */
@Component
public class DocumentParseJobCleanupListener {

    private static final Logger log = LoggerFactory.getLogger(DocumentParseJobCleanupListener.class);
    private static final List<JobStatus> GHOST_STATUSES = List.of(JobStatus.PENDING, JobStatus.PROCESSING);

    private final DocumentParseJobRepository jobRepository;

    public DocumentParseJobCleanupListener(DocumentParseJobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void cleanupGhostJobs() {
        List<DocumentParseJob> ghosts = jobRepository.findAllByStatusIn(GHOST_STATUSES);
        if (ghosts.isEmpty()) {
            return;
        }
        ghosts.forEach(job -> job.fail("서버 재시작으로 작업이 중단되었습니다."));
        jobRepository.saveAll(ghosts);
        log.info("유령 파싱 job {}건을 FAILED로 정리했습니다.", ghosts.size());
    }
}
