package com.example.docsuriserver.hscode.entity;

import java.math.BigDecimal;

/**
 * koreanName/englishName은 반드시 hs_codes 테이블 조회값으로 채운다. LLM 생성값을 쓰지 않는다.
 */
public record HsCodeCandidate(
        int rank,
        String hsCode,
        String koreanName,
        String englishName,
        BigDecimal confidence,
        String reason
) {
}
