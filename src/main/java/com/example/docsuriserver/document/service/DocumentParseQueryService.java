package com.example.docsuriserver.document.service;

import com.example.docsuriserver.common.ConflictException;
import com.example.docsuriserver.common.NotFoundException;
import com.example.docsuriserver.document.entity.DocumentParseJob;
import com.example.docsuriserver.document.entity.ExtractedDocument;
import com.example.docsuriserver.document.repository.DocumentParseJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 다른 도메인(Validation)이 파싱 완료 결과가 필요할 때 이 서비스를 주입한다.
 * 리포지토리를 직접 주입하지 않는다 (00-CONVENTIONS.md 1절).
 */
@Service
public class DocumentParseQueryService {

    private final DocumentParseJobRepository jobRepository;

    public DocumentParseQueryService(DocumentParseJobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional(readOnly = true)
    public List<ExtractedDocument> getCompletedExtractions(UUID sessionId) {
        DocumentParseJob job = jobRepository.findFirstBySessionIdOrderByCreatedAtDesc(sessionId)
                .orElseThrow(() -> new NotFoundException("해당 세션의 파싱 작업을 찾을 수 없습니다."));
        if (!job.isCompleted()) {
            throw new ConflictException("파싱이 완료되지 않았습니다. 현재 상태: " + job.getStatus());
        }
        return job.getExtractionDocuments() == null ? List.of() : job.getExtractionDocuments();
    }
}
