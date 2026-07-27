package com.example.docsuriserver.guide.entity;

import com.example.docsuriserver.common.DisclaimerPosition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "disclaimers")
public class Disclaimer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "disclaimer_id")
    private Long disclaimerId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "display_position", nullable = false, length = 50)
    private DisclaimerPosition displayPosition;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected Disclaimer() {
    }

    public Long getDisclaimerId() {
        return disclaimerId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public DisclaimerPosition getDisplayPosition() {
        return displayPosition;
    }

    public boolean isActive() {
        return active;
    }
}
