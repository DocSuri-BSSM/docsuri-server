package com.example.docsuriserver.correction.entity;

import com.example.docsuriserver.common.CorrectionStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "correction_requests")
public class CorrectionRequest {

    @Id
    @GeneratedValue
    @Column(name = "correction_request_id")
    private UUID correctionRequestId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "validation_run_id", nullable = false)
    private UUID validationRunId;

    @Column(name = "output_language", nullable = false, length = 10)
    private String outputLanguage;

    @Column(name = "additional_instruction", columnDefinition = "text")
    private String additionalInstruction;

    @Column(name = "title")
    private String title;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variables", columnDefinition = "jsonb")
    private List<CorrectionVariable> variables;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CorrectionStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected CorrectionRequest() {
    }

    public static CorrectionRequest create(UUID sessionId, UUID validationRunId, String outputLanguage,
                                           String additionalInstruction, String title, String content,
                                           List<CorrectionVariable> variables) {
        CorrectionRequest r = new CorrectionRequest();
        r.sessionId = sessionId;
        r.validationRunId = validationRunId;
        r.outputLanguage = outputLanguage;
        r.additionalInstruction = additionalInstruction;
        r.title = title;
        r.content = content;
        r.variables = variables;
        r.status = CorrectionStatus.DRAFT;
        r.createdAt = LocalDateTime.now();
        r.updatedAt = LocalDateTime.now();
        return r;
    }

    public void updateTitle(String title) {
        this.title = title;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateContent(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateVariables(List<CorrectionVariable> variables) {
        this.variables = variables;
        this.updatedAt = LocalDateTime.now();
    }

    /** required 변수가 모두 채워졌으면 CONFIRMED로, 아니면 DRAFT로 되돌린다. */
    public void refreshConfirmationStatus() {
        boolean allRequiredFilled = variables == null || variables.stream()
                .filter(CorrectionVariable::required)
                .allMatch(v -> !isPlaceholder(v.value()));
        this.status = allRequiredFilled ? CorrectionStatus.CONFIRMED : CorrectionStatus.DRAFT;
    }

    public void markExported() {
        this.status = CorrectionStatus.EXPORTED;
        this.updatedAt = LocalDateTime.now();
    }

    public static boolean isPlaceholder(String value) {
        return value == null || value.isBlank() || (value.startsWith("[") && value.endsWith("]"));
    }

    public UUID getCorrectionRequestId() {
        return correctionRequestId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getValidationRunId() {
        return validationRunId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public List<CorrectionVariable> getVariables() {
        return variables;
    }

    public CorrectionStatus getStatus() {
        return status;
    }
}
