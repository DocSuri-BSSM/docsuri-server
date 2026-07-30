package com.example.docsuriserver.validation.dto;

import com.example.docsuriserver.common.ValidationRule;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.List;

public record ValidationCreateRequest(
        @NotEmpty List<ValidationRule> rules,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal weightTolerancePercent,
        @NotNull @Pattern(regexp = "KO|EN") String outputLanguage
) {
}
