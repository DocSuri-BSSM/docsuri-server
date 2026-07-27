package com.example.docsuriserver.validation.service;

import com.example.docsuriserver.common.ConflictException;
import com.example.docsuriserver.common.InvalidRequestException;
import com.example.docsuriserver.common.NotFoundException;
import com.example.docsuriserver.validation.entity.ValidationRun;
import com.example.docsuriserver.validation.repository.ValidationRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 다른 도메인(Correction)이 검증 결과가 필요할 때 이 서비스를 주입한다.
 * ValidationRepository를 직접 주입하지 않는다 (00-CONVENTIONS.md 1절 예시).
 */
@Service
public class ValidationQueryService {

    private final ValidationRunRepository runRepository;

    public ValidationQueryService(ValidationRunRepository runRepository) {
        this.runRepository = runRepository;
    }

    @Transactional(readOnly = true)
    public ValidationRun getCompletedRun(UUID validationRunId, UUID sessionId) {
        ValidationRun run = getById(validationRunId);
        if (!run.getSessionId().equals(sessionId)) {
            throw new InvalidRequestException("validation_run_id가 해당 세션 소속이 아닙니다.");
        }
        if (!run.isCompleted()) {
            throw new ConflictException("검증이 완료되지 않았습니다. 현재 상태: " + run.getStatus());
        }
        return run;
    }

    @Transactional(readOnly = true)
    public ValidationRun getById(UUID validationRunId) {
        return runRepository.findById(validationRunId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 검증 작업입니다."));
    }
}
