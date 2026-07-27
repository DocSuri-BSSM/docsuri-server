package com.example.docsuriserver.document.service;

import com.example.docsuriserver.common.ConflictException;
import com.example.docsuriserver.common.InvalidRequestException;
import com.example.docsuriserver.common.JobStatus;
import com.example.docsuriserver.common.NotFoundException;
import com.example.docsuriserver.document.dto.DocumentExtractionListSearchResponse;
import com.example.docsuriserver.document.dto.DocumentParseStartRequest;
import com.example.docsuriserver.document.dto.DocumentParseStartResponse;
import com.example.docsuriserver.document.dto.DocumentParseStatusSearchResponse;
import com.example.docsuriserver.document.entity.DocumentParseJob;
import com.example.docsuriserver.document.repository.DocumentParseJobRepository;
import com.example.docsuriserver.document.repository.DocumentRepository;
import com.example.docsuriserver.document.repository.DocumentSessionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentParseService {

    private static final List<JobStatus> ACTIVE_STATUSES = List.of(JobStatus.PENDING, JobStatus.PROCESSING);

    private final DocumentSessionRepository sessionRepository;
    private final DocumentRepository documentRepository;
    private final DocumentParseJobRepository jobRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DocumentParseService(DocumentSessionRepository sessionRepository,
                                DocumentRepository documentRepository,
                                DocumentParseJobRepository jobRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.sessionRepository = sessionRepository;
        this.documentRepository = documentRepository;
        this.jobRepository = jobRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public DocumentParseStartResponse start(UUID sessionId, DocumentParseStartRequest request) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new NotFoundException("존재하지 않는 세션입니다.");
        }
        if (documentRepository.findAllBySessionId(sessionId).isEmpty()) {
            throw new InvalidRequestException("세션에 업로드된 서류가 없습니다.");
        }
        if (jobRepository.existsBySessionIdAndStatusIn(sessionId, ACTIVE_STATUSES)) {
            throw new ConflictException("이미 진행 중인 파싱 작업이 있습니다.");
        }

        DocumentParseJob job = jobRepository.save(
                DocumentParseJob.create(sessionId, request.ocrLanguage(), request.extractFields()));

        eventPublisher.publishEvent(new DocumentParseJobCreatedEvent(job.getParseJobId()));

        return new DocumentParseStartResponse(job.getParseJobId());
    }

    @Transactional(readOnly = true)
    public DocumentParseStatusSearchResponse getStatus(UUID sessionId) {
        return DocumentParseStatusSearchResponse.from(findLatestJob(sessionId));
    }

    @Transactional(readOnly = true)
    public DocumentExtractionListSearchResponse getExtractions(UUID sessionId) {
        DocumentParseJob job = findLatestJob(sessionId);
        if (!job.isCompleted()) {
            throw new ConflictException("아직 파싱이 완료되지 않았습니다. 현재 상태: " + job.getStatus());
        }
        List<com.example.docsuriserver.document.entity.ExtractedDocument> results =
                job.getExtractionDocuments() == null ? List.of() : job.getExtractionDocuments();
        return DocumentExtractionListSearchResponse.from(results);
    }

    private DocumentParseJob findLatestJob(UUID sessionId) {
        return jobRepository.findFirstBySessionIdOrderByCreatedAtDesc(sessionId)
                .orElseThrow(() -> new NotFoundException("해당 세션의 파싱 작업을 찾을 수 없습니다."));
    }
}
