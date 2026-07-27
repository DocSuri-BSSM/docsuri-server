package com.example.docsuriserver.hscode.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "hs_codes")
public class HsCode {

    @Id
    @Column(name = "hs_code", length = 20)
    private String hsCode;

    @Column(name = "korean_name", nullable = false)
    private String koreanName;

    @Column(name = "english_name")
    private String englishName;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "tariff_rate", precision = 10, scale = 4)
    private BigDecimal tariffRate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "import_requirements", columnDefinition = "jsonb")
    private List<String> importRequirements;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected HsCode() {
    }

    public String getHsCode() {
        return hsCode;
    }

    public String getKoreanName() {
        return koreanName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getTariffRate() {
        return tariffRate;
    }

    public List<String> getImportRequirements() {
        return importRequirements;
    }
}
