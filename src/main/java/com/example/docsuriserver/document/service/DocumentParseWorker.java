package com.example.docsuriserver.document.service;

import com.example.docsuriserver.document.entity.Document;
import com.example.docsuriserver.document.entity.DocumentParseJob;
import com.example.docsuriserver.document.entity.ExtractedDocument;
import com.example.docsuriserver.document.entity.ParseStep;
import com.example.docsuriserver.document.repository.DocumentParseJobRepository;
import com.example.docsuriserver.document.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @Async 는 self-invocation 시 동작하지 않으므로, 파싱 실행부를 별도 빈으로 분리한다.
 * @TransactionalEventListener(AFTER_COMMIT) 로 커밋 이후에만 실행되어, 아직 커밋되지 않은 row를
 * 조회하다 실패하는 문제를 막는다.
 * 각 단계마다 save() 해서 parse-status 폴링이 진행률을 볼 수 있게 한다.
 */
@Component
public class DocumentParseWorker {

    private static final Logger log = LoggerFactory.getLogger(DocumentParseWorker.class);

    private final DocumentParseJobRepository jobRepository;
    private final DocumentRepository documentRepository;
    private final FileStorage fileStorage;
    private final OcrClient ocrClient;

    public DocumentParseWorker(DocumentParseJobRepository jobRepository,
                               DocumentRepository documentRepository,
                               FileStorage fileStorage,
                               OcrClient ocrClient) {
        this.jobRepository = jobRepository;
        this.documentRepository = documentRepository;
        this.fileStorage = fileStorage;
        this.ocrClient = ocrClient;
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(DocumentParseJobCreatedEvent event) {
        run(event.parseJobId());
    }

    private void run(UUID parseJobId) {
        DocumentParseJob job = jobRepository.findById(parseJobId).orElseThrow();
        try {
            job.progress(ParseStep.PREPROCESSING, 10);
            jobRepository.save(job);

            List<Document> documents = documentRepository.findAllBySessionId(job.getSessionId());

            job.progress(ParseStep.OCR, 30);
            jobRepository.save(job);

            List<ExtractedDocument> results = new ArrayList<>();
            int total = documents.size();
            for (int i = 0; i < total; i++) {
                Document document = documents.get(i);
                List<String> extractFields = job.getExtractFields()
                        .getOrDefault(document.getDocumentType(), List.of());
                byte[] content = fileStorage.load(document.getFileUrl().substring("/files/".length()));
                OcrRequest request = new OcrRequest(
                        content, document.getContentType(), document.getDocumentType(),
                        job.getOcrLanguage(), extractFields);

                var fields = ocrClient.extract(request);
                results.add(new ExtractedDocument(document.getDocumentId(), document.getDocumentType(), fields));

                int percent = 30 + (int) ((i + 1) / (double) total * 60);
                job.progress(ParseStep.FIELD_EXTRACTION, percent);
                jobRepository.save(job);
            }

            job.complete(results);
            jobRepository.save(job);
        } catch (Exception e) {
            log.error("문서 파싱 실패 parseJobId={}", parseJobId, e);
            job.fail("문서 파싱 중 오류가 발생했습니다.");
            jobRepository.save(job);
        }
    }
}
