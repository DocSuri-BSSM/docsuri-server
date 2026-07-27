package com.example.docsuriserver.document.service;

import com.example.docsuriserver.common.NotFoundException;
import com.example.docsuriserver.document.repository.DocumentSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 다른 도메인(Validation/Correction)이 세션 존재 여부만 확인할 때 이 서비스를 주입한다.
 * 리포지토리를 직접 주입하지 않는다 (00-CONVENTIONS.md 1절).
 */
@Service
public class DocumentSessionQueryService {

    private final DocumentSessionRepository sessionRepository;

    public DocumentSessionQueryService(DocumentSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional(readOnly = true)
    public void requireExists(UUID sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new NotFoundException("존재하지 않는 세션입니다.");
        }
    }
}
