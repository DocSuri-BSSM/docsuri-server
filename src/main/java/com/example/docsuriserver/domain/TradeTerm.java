package com.example.docsuriserver.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 무역 용어 사전 (INCOTERMS 등). 자연키(term)를 PK로 사용한다.
 */
@Entity
@Table(name = "trade_terms")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TradeTerm {

    @Id
    @Column(name = "term", length = 100, nullable = false)
    private String term;

    @Column(name = "full_name", length = 255, nullable = false)
    private String fullName;

    @Column(name = "korean_name", length = 255, nullable = false)
    private String koreanName;

    @Column(name = "description", columnDefinition = "text", nullable = false)
    private String description;
}
