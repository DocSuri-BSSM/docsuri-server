package com.example.docsuriserver.document.service;

import com.example.docsuriserver.common.DocumentType;
import com.example.docsuriserver.common.InvalidRequestException;
import com.example.docsuriserver.document.dto.DocumentUploadResponse;
import com.example.docsuriserver.document.entity.Document;
import com.example.docsuriserver.document.entity.DocumentSession;
import com.example.docsuriserver.document.repository.DocumentRepository;
import com.example.docsuriserver.document.repository.DocumentSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentUploadService {

    private static final Logger log = LoggerFactory.getLogger(DocumentUploadService.class);

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("application/pdf", "image/png", "image/jpeg");

    private final DocumentSessionRepository sessionRepository;
    private final DocumentRepository documentRepository;
    private final FileStorage fileStorage;

    public DocumentUploadService(DocumentSessionRepository sessionRepository,
                                 DocumentRepository documentRepository,
                                 FileStorage fileStorage) {
        this.sessionRepository = sessionRepository;
        this.documentRepository = documentRepository;
        this.fileStorage = fileStorage;
    }

    @Transactional
    public DocumentUploadResponse upload(MultipartFile invoiceFile,
                                         MultipartFile billOfLadingFile,
                                         MultipartFile packingListFile) {

        Map<DocumentType, MultipartFile> files = new LinkedHashMap<>();
        putIfPresent(files, DocumentType.INVOICE, invoiceFile);
        putIfPresent(files, DocumentType.BILL_OF_LADING, billOfLadingFile);
        putIfPresent(files, DocumentType.PACKING_LIST, packingListFile);

        if (files.isEmpty()) {
            throw new InvalidRequestException("최소 1개 이상의 서류를 업로드해야 합니다.");
        }
        files.values().forEach(this::validateFile);

        DocumentSession session = sessionRepository.save(DocumentSession.create());
        UUID sessionId = session.getSessionId();

        List<String> storedFileKeys = new ArrayList<>();
        try {
            List<UUID> documentIds = new ArrayList<>();
            files.forEach((type, file) -> {
                StoredFile stored = fileStorage.store(sessionId.toString(), file);
                storedFileKeys.add(stored.fileKey());
                String fileUrl = "/files/" + stored.fileKey();
                Document saved = documentRepository.save(
                        Document.create(sessionId, type, file.getOriginalFilename(), fileUrl,
                                stored.contentType(), stored.size()));
                documentIds.add(saved.getDocumentId());
            });
            return new DocumentUploadResponse(sessionId, documentIds);
        } catch (RuntimeException e) {
            // 트랜잭션은 롤백되지만 디스크에 이미 쓴 파일은 남으므로 직접 정리한다.
            storedFileKeys.forEach(fileKey -> {
                try {
                    fileStorage.delete(fileKey);
                } catch (RuntimeException deleteFailure) {
                    log.warn("업로드 실패 후 파일 정리 실패: {}", fileKey, deleteFailure);
                }
            });
            throw e;
        }
    }

    private void validateFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidRequestException("허용되지 않는 파일 형식입니다: " + contentType);
        }
    }

    private void putIfPresent(Map<DocumentType, MultipartFile> map, DocumentType type, MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            map.put(type, file);
        }
    }
}
