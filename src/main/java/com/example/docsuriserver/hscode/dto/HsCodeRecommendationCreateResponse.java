package com.example.docsuriserver.hscode.dto;

import com.example.docsuriserver.hscode.entity.HsCodeCandidate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record HsCodeRecommendationCreateResponse(
        UUID recommendationId,
        List<CandidateItem> candidates,
        String disclaimer
) {

    public record CandidateItem(
            int rank,
            String hsCode,
            String koreanName,
            String englishName,
            BigDecimal confidence,
            String reason
    ) {
    }

    public static HsCodeRecommendationCreateResponse of(UUID recommendationId, List<HsCodeCandidate> candidates, String disclaimer) {
        List<CandidateItem> items = candidates.stream()
                .map(c -> new CandidateItem(c.rank(), c.hsCode(), c.koreanName(), c.englishName(), c.confidence(), c.reason()))
                .toList();
        return new HsCodeRecommendationCreateResponse(recommendationId, items, disclaimer);
    }
}
