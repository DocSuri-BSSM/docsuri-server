package com.example.docsuriserver.correction.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.example.docsuriserver.common.ConflictException;
import com.example.docsuriserver.common.DisclaimerPosition;
import com.example.docsuriserver.common.ExportFormat;
import com.example.docsuriserver.common.NotFoundException;
import com.example.docsuriserver.correction.dto.CorrectionRequestExportRequest;
import com.example.docsuriserver.correction.dto.CorrectionRequestExportResponse;
import com.example.docsuriserver.correction.entity.CorrectionRequest;
import com.example.docsuriserver.correction.entity.CorrectionRequestExport;
import com.example.docsuriserver.correction.entity.CorrectionVariable;
import com.example.docsuriserver.correction.repository.CorrectionRequestExportRepository;
import com.example.docsuriserver.correction.repository.CorrectionRequestRepository;
import com.example.docsuriserver.document.service.FileStorage;
import com.example.docsuriserver.document.service.StoredFile;
import com.example.docsuriserver.guide.service.DisclaimerQueryService;
import com.example.docsuriserver.validation.entity.ValidationIssue;
import com.example.docsuriserver.validation.entity.ValidationRun;
import com.example.docsuriserver.validation.service.ValidationQueryService;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * PDF는 NanumBarunGothic 폰트를 임베딩해 한글이 깨지지 않게 하고, DOCX는 뷰어(Word/한글 등)의
 * 시스템 폰트에 의존한다 (POI는 글리프를 파일에 굽지 않으므로 별도 임베딩이 필요 없음).
 */
@Service
public class CorrectionRequestExportService {

    private static final String KOREAN_FONT_RESOURCE = "/fonts/NanumBarunGothic.ttf";
    private static final String KOREAN_FONT_FAMILY = "NanumBarunGothic";

    private final CorrectionRequestRepository correctionRequestRepository;
    private final CorrectionRequestExportRepository exportRepository;
    private final ValidationQueryService validationQueryService;
    private final DisclaimerQueryService disclaimerQueryService;
    private final FileStorage fileStorage;
    private final int ttlHours;

    public CorrectionRequestExportService(CorrectionRequestRepository correctionRequestRepository,
                                          CorrectionRequestExportRepository exportRepository,
                                          ValidationQueryService validationQueryService,
                                          DisclaimerQueryService disclaimerQueryService,
                                          FileStorage fileStorage,
                                          @Value("${app.export.ttl-hours:24}") int ttlHours) {
        this.correctionRequestRepository = correctionRequestRepository;
        this.exportRepository = exportRepository;
        this.validationQueryService = validationQueryService;
        this.disclaimerQueryService = disclaimerQueryService;
        this.fileStorage = fileStorage;
        this.ttlHours = ttlHours;
    }

    @Transactional
    public CorrectionRequestExportResponse export(UUID correctionRequestId, CorrectionRequestExportRequest request) {
        CorrectionRequest r = correctionRequestRepository.findById(correctionRequestId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 정정 요청서입니다."));

        List<CorrectionVariable> variables = r.getVariables() == null ? List.of() : r.getVariables();
        boolean hasUnfilledRequired = variables.stream()
                .anyMatch(v -> v.required() && CorrectionRequest.isPlaceholder(v.value()));
        if (hasUnfilledRequired) {
            throw new ConflictException("입력이 필요한 항목이 남아 있습니다.");
        }

        String disclaimer = disclaimerQueryService.getContent(DisclaimerPosition.CORRECTION);
        ValidationRun validationRun = request.includeValidationReport()
                ? validationQueryService.getById(r.getValidationRunId())
                : null;

        byte[] bytes;
        String contentType;
        String extension;
        if (request.format() == ExportFormat.PDF) {
            bytes = renderPdf(buildHtml(r, validationRun, disclaimer, request.includeValidationReport()));
            contentType = "application/pdf";
            extension = "pdf";
        } else {
            bytes = renderDocx(r, validationRun, disclaimer, request.includeValidationReport());
            contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            extension = "docx";
        }

        String fileName = sanitizeFileName(r.getTitle()) + "." + extension;
        StoredFile stored = fileStorage.store("exports", bytes, fileName, contentType);
        String downloadUrl = "/files/" + stored.fileKey();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(ttlHours);

        CorrectionRequestExport export = CorrectionRequestExport.create(
                correctionRequestId, request.format(), request.includeValidationReport(),
                fileName, stored.fileKey(), downloadUrl, expiresAt);
        exportRepository.save(export);

        r.markExported();

        return new CorrectionRequestExportResponse(export.getExportId(), fileName, downloadUrl, expiresAt);
    }

    private String sanitizeFileName(String title) {
        String base = (title == null || title.isBlank()) ? "correction_request" : title;
        return base.trim().replaceAll("[^A-Za-z0-9._\\uAC00-\\uD7A3-]", "_");
    }

    // ---- PDF ----

    byte[] renderPdf(String html) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.useFont(() -> getClass().getResourceAsStream(KOREAN_FONT_RESOURCE), KOREAN_FONT_FAMILY);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("PDF 생성 실패", e);
        }
    }

    String buildHtml(CorrectionRequest r, ValidationRun validationRun, String disclaimer, boolean includeReport) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset='utf-8'/><style>")
                .append("body { font-family: '").append(KOREAN_FONT_FAMILY).append("', sans-serif; font-size: 12px; line-height: 1.6; }")
                .append("h1 { font-size: 18px; } h2 { font-size: 15px; margin-top: 24px; }")
                .append(".content { white-space: pre-wrap; }")
                .append(".disclaimer { margin-top: 32px; font-size: 10px; color: #666; border-top: 1px solid #ccc; padding-top: 8px; }")
                .append("</style></head><body>")
                .append("<h1>").append(escapeHtml(r.getTitle())).append("</h1>")
                .append("<div class='content'>").append(escapeHtml(r.getContent())).append("</div>");

        if (includeReport && validationRun != null) {
            html.append("<h2>교차검증 결과</h2>").append(buildValidationReportHtml(validationRun));
        }

        html.append("<div class='disclaimer'>").append(escapeHtml(disclaimer == null ? "" : disclaimer)).append("</div>")
                .append("</body></html>");
        return html.toString();
    }

    private String buildValidationReportHtml(ValidationRun run) {
        StringBuilder html = new StringBuilder();
        html.append("<p>종합 신호: ").append(run.getOverallSignal())
                .append(" (정상 ").append(run.getNormalCount())
                .append(" / 주의 ").append(run.getWarningCount())
                .append(" / 오류 ").append(run.getErrorCount()).append(")</p>");

        List<ValidationIssue> issues = run.getIssues() == null ? List.of() : run.getIssues();
        for (ValidationIssue issue : issues) {
            html.append("<p><b>[").append(issue.status()).append("] ").append(escapeHtml(issue.title())).append("</b><br/>")
                    .append(escapeHtml(issue.subtitle())).append("<br/>")
                    .append(escapeHtml(issue.cause())).append("</p>");
        }
        return html.toString();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ---- DOCX ----

    byte[] renderDocx(CorrectionRequest r, ValidationRun validationRun, String disclaimer, boolean includeReport) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            writeParagraph(doc, r.getTitle(), true, 16);
            writeParagraph(doc, r.getContent(), false, 11);

            if (includeReport && validationRun != null) {
                writeParagraph(doc, "교차검증 결과", true, 13);
                writeParagraph(doc, String.format("종합 신호: %s (정상 %d / 주의 %d / 오류 %d)",
                        validationRun.getOverallSignal(), validationRun.getNormalCount(),
                        validationRun.getWarningCount(), validationRun.getErrorCount()), false, 11);

                List<ValidationIssue> issues = validationRun.getIssues() == null ? List.of() : validationRun.getIssues();
                for (ValidationIssue issue : issues) {
                    writeParagraph(doc, "[" + issue.status() + "] " + issue.title(), true, 11);
                    writeParagraph(doc, issue.subtitle() + "\n" + issue.cause(), false, 10);
                }
            }

            writeParagraph(doc, disclaimer == null ? "" : disclaimer, false, 8);

            doc.write(os);
            return os.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("DOCX 생성 실패", e);
        }
    }

    private void writeParagraph(XWPFDocument doc, String text, boolean bold, int fontSize) {
        XWPFParagraph paragraph = doc.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setFontFamily("맑은 고딕");
        run.setBold(bold);
        run.setFontSize(fontSize);
        String content = text == null ? "" : text;
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                run.addBreak();
            }
            run.setText(lines[i]);
        }
    }
}
