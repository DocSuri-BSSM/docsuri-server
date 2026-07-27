package com.example.docsuriserver.correction.service;

import com.example.docsuriserver.correction.entity.CorrectionRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 05-TASKS.md T8 완료조건: "생성된 PDF를 열었을 때 한글이 깨지지 않는다."
 * 실제 서비스 인스턴스(DB 없이)로 렌더링 메서드만 호출해 폰트 임베딩이 실제로 동작하는지 확인한다.
 */
class CorrectionRequestExportServiceTest {

    private final CorrectionRequestExportService service =
            new CorrectionRequestExportService(null, null, null, null, null, 24);

    private CorrectionRequest sampleRequest() {
        return CorrectionRequest.create(
                UUID.randomUUID(), UUID.randomUUID(), "KO", null,
                "B/L 총중량 정정 요청서",
                "B/L 총중량의 정정을 요청드립니다. 당사 Commercial Invoice 및 Packing List상 실제 총중량은 12,500KG이나, B/L에 11,800KG으로 상이하게 기재되어 정정을 요청드립니다.",
                java.util.List.of());
    }

    @Test
    void pdfEmbedsKoreanTextReadably() throws Exception {
        CorrectionRequest request = sampleRequest();
        String html = service.buildHtml(request, null, "본 문서는 AI가 생성한 초안입니다.", false);
        byte[] pdfBytes = service.renderPdf(html);

        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, 0, 4)).isEqualTo("%PDF");

        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            String extracted = new PDFTextStripper().getText(doc);
            assertThat(extracted).contains("총중량 정정 요청서");
            assertThat(extracted).contains("정정을 요청드립니다");
        }
    }

    @Test
    void docxContainsKoreanText() throws Exception {
        CorrectionRequest request = sampleRequest();
        byte[] docxBytes = service.renderDocx(request, null, "본 문서는 AI가 생성한 초안입니다.", false);

        assertThat(docxBytes).isNotEmpty();
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            String allText = doc.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .reduce("", (a, b) -> a + b);
            assertThat(allText).contains("총중량 정정 요청서");
            assertThat(allText).contains("정정을 요청드립니다");
        }
    }
}
