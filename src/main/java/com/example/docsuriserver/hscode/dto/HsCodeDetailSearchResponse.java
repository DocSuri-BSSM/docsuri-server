package com.example.docsuriserver.hscode.dto;

import com.example.docsuriserver.hscode.entity.HsCode;

import java.math.BigDecimal;
import java.util.List;

public record HsCodeDetailSearchResponse(
        String hsCode,
        String koreanName,
        String englishName,
        String description,
        BigDecimal tariffRate,
        List<String> importRequirements
) {
    public static HsCodeDetailSearchResponse from(HsCode entity) {
        return new HsCodeDetailSearchResponse(
                entity.getHsCode(), entity.getKoreanName(), entity.getEnglishName(), entity.getDescription(),
                entity.getTariffRate(),
                entity.getImportRequirements() == null ? List.of() : entity.getImportRequirements());
    }
}
