package com.example.docsuriserver.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 화면에 노출되는 면책/고지 문구.
 */
@Entity
@Table(name = "disclaimers")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Disclaimer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "disclaimer_id")
    private Long disclaimerId;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "text", nullable = false)
    private String content;

    @Column(name = "display_position", length = 50, nullable = false)
    private String displayPosition;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
