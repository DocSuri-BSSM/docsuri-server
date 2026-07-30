package com.example.docsuriserver.validation.service;

import com.example.docsuriserver.common.ConflictException;
import com.example.docsuriserver.common.JobStatus;
import com.example.docsuriserver.common.NotFoundException;
import com.example.docsuriserver.document.service.DocumentParseQueryService;
import com.example.docsuriserver.document.service.DocumentSessionQueryService;
import com.example.docsuriserver.validation.dto.ValidationCreateRequest;
import com.example.docsuriserver.validation.dto.ValidationCreateResponse;
import com.example.docsuriserver.validation.dto.ValidationResultSearchResponse;
import com.example.docsuriserver.validation.entity.ValidationRun;
import com.example.docsuriserver.validation.repository.ValidationRunRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ValidationService {

    private static final List<JobStatus> ACTIVE_STATUSES = List.of(JobStatus.PENDING, JobStatus.PROCESSING);

    private final ValidationRunRepository runRepository;
    private final DocumentSessionQueryService sessionQueryService;
    private final DocumentParseQueryService parseQueryService;
    private final ApplicationEventPublisher eventPublisher;

    public ValidationService(ValidationRunRepository runRepository,
                             DocumentSessionQueryService sessionQueryService,
                             DocumentParseQueryService parseQueryService,
                             ApplicationEventPublisher eventPublisher) {
        this.runRepository = runRepository;
        this.sessionQueryService = sessionQueryService;
        this.parseQueryService = parseQueryService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ValidationCreateResponse start(UUID sessionId, ValidationCreateRequest request) {
        sessionQueryService.requireExists(sessionId);
        parseQueryService.getCompletedExtractions(sessionId); // 파싱 완료 여부 사전 검증 (404/409)
        if (runRepository.existsBySessionIdAndStatusIn(sessionId, ACTIVE_STATUSES)) {
            throw new ConflictException("이미 진행 중인 검증 작업이 있습니다.");
        }

        ValidationRun run = runRepository.save(
                ValidationRun.create(sessionId, request.rules(), request.weightTolerancePercent(), request.outputLanguage()));

        eventPublisher.publishEvent(new ValidationRunCreatedEvent(run.getValidationRunId()));

        return new ValidationCreateResponse(run.getValidationRunId());
    }

    @Transactional(readOnly = true)
    public ValidationResultSearchResponse getResult(UUID sessionId) {
        ValidationRun run = runRepository.findFirstBySessionIdOrderByCreatedAtDesc(sessionId)
                .orElseThrow(() -> new NotFoundException("해당 세션의 검증 작업을 찾을 수 없습니다."));
        return ValidationResultSearchResponse.from(run);
    }
}
