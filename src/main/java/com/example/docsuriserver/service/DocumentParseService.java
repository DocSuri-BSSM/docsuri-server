package com.example.docsuriserver.service;

import com.example.docsuriserver.common.NotFoundException;
import com.example.docsuriserver.dto.DocumentExtractionListSearchResponse;
import com.example.docsuriserver.dto.DocumentParseStartRequest;
import com.example.docsuriserver.dto.DocumentParseStartResponse;
import com.example.docsuriserver.dto.DocumentParseStatusSearchResponse;
import com.example.docsuriserver.entity.DocumentParseJob;
import com.example.docsuriserver.repository.DocumentParseJobRepository;
import com.example.docsuriserver.repository.DocumentRepository;
import com.example.docsuriserver.repository.DocumentSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentParseService {

    private final DocumentSessionRepository sessionRepository;
    private final DocumentRepository documentRepository;
    private final DocumentParseJobRepository jobRepository;
    private final DocumentParseWorker worker;

    public DocumentParseService(DocumentSessionRepository sessionRepository,
                                DocumentRepository documentRepository,
                                DocumentParseJobRepository jobRepository,
                                DocumentParseWorker worker) {
        this.sessionRepository = sessionRepository;
        this.documentRepository = documentRepository;
        this.jobRepository = jobRepository;
        this.worker = worker;
    }

    @Transactional
    public DocumentParseStartResponse start(UUID sessionId, DocumentParseStartRequest request) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new NotFoundException("존재하지 않는 세션입니다.");
        }
        if (documentRepository.findAllBySessionId(sessionId).isEmpty()) {
            throw new IllegalArgumentException("세션에 업로드된 서류가 없습니다.");
        }

        DocumentParseJob job = jobRepository.save(
                DocumentParseJob.create(sessionId, request.ocrLanguage(), request.extractFields()));

        // 커밋 이후 실행하고 싶다면 TransactionSynchronizationManager 또는 @TransactionalEventListener 사용
        worker.run(job.getParseJobId());

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
            throw new IllegalArgumentException("아직 파싱이 완료되지 않았습니다. 현재 상태: " + job.getStatus());
        }
        List<com.example.docsuriserver.entity.ExtractedDocument> results =
                job.getExtractionDocuments() == null ? List.of() : job.getExtractionDocuments();
        return DocumentExtractionListSearchResponse.from(results);
    }

    private DocumentParseJob findLatestJob(UUID sessionId) {
        return jobRepository.findFirstBySessionIdOrderByCreatedAtDesc(sessionId)
                .orElseThrow(() -> new NotFoundException("해당 세션의 파싱 작업을 찾을 수 없습니다."));
    }
}
