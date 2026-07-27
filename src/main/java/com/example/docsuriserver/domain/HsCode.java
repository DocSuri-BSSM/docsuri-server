package com.example.docsuriserver.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * HS 코드(품목분류) 및 관세율/수입요건 정보.
 */
@Entity
@Table(name = "hs_codes")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class HsCode {

    @Id
    @Column(name = "hs_code", length = 20, nullable = false)
    private String hsCode;

    @Column(name = "korean_name", length = 255, nullable = false)
    private String koreanName;

    @Column(name = "english_name", length = 255, nullable = false)
    private String englishName;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "tariff_rate", precision = 10, scale = 4)
    private BigDecimal tariffRate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "import_requirements", columnDefinition = "jsonb")
    private JsonNode importRequirements;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
