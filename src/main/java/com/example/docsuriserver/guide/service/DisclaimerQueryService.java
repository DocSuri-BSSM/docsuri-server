package com.example.docsuriserver.guide.service;

import com.example.docsuriserver.common.DisclaimerPosition;
import com.example.docsuriserver.guide.dto.DisclaimerListSearchResponse;
import com.example.docsuriserver.guide.repository.DisclaimerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다른 도메인(Validation/HsCode/Correction)이 면책 문구가 필요할 때 이 서비스를 주입한다.
 * 리포지토리를 직접 주입하지 않는다 (00-CONVENTIONS.md 1절).
 */
@Service
public class DisclaimerQueryService {

    private final DisclaimerRepository disclaimerRepository;

    public DisclaimerQueryService(DisclaimerRepository disclaimerRepository) {
        this.disclaimerRepository = disclaimerRepository;
    }

    @Transactional(readOnly = true)
    public DisclaimerListSearchResponse listActive() {
        return DisclaimerListSearchResponse.from(disclaimerRepository.findAllByActiveTrueOrderByDisclaimerIdAsc());
    }

    /** 해당 position의 활성 면책 문구를 찾고, 없으면 GLOBAL로 폴백한다. */
    @Transactional(readOnly = true)
    public String getContent(DisclaimerPosition position) {
        return disclaimerRepository.findFirstByDisplayPositionAndActiveTrue(position)
                .or(() -> disclaimerRepository.findFirstByDisplayPositionAndActiveTrue(DisclaimerPosition.GLOBAL))
                .map(com.example.docsuriserver.guide.entity.Disclaimer::getContent)
                .orElse(null);
    }
}
