package com.example.docsuriserver.guide.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "trade_terms")
public class TradeTerm {

    @Id
    @Column(name = "term", length = 100)
    private String term;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "korean_name", nullable = false)
    private String koreanName;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    protected TradeTerm() {
    }

    public String getTerm() {
        return term;
    }

    public String getFullName() {
        return fullName;
    }

    public String getKoreanName() {
        return koreanName;
    }

    public String getDescription() {
        return description;
    }
}
