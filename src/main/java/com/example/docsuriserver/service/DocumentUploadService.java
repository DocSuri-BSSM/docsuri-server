package com.example.docsuriserver.service;

import com.example.docsuriserver.dto.DocumentUploadResponse;
import com.example.docsuriserver.entity.Document;
import com.example.docsuriserver.entity.DocumentSession;
import com.example.docsuriserver.entity.DocumentType;
import com.example.docsuriserver.repository.DocumentRepository;
import com.example.docsuriserver.repository.DocumentSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentUploadService {

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
            throw new IllegalArgumentException("최소 1개 이상의 서류를 업로드해야 합니다.");
        }

        DocumentSession session = sessionRepository.save(DocumentSession.create());
        UUID sessionId = session.getSessionId();

        List<UUID> documentIds = new ArrayList<>();
        files.forEach((type, file) -> {
            String fileUrl = fileStorage.store(sessionId, file);
            Document saved = documentRepository.save(
                    Document.create(sessionId, type, file.getOriginalFilename(), fileUrl));
            documentIds.add(saved.getDocumentId());
        });

        return new DocumentUploadResponse(sessionId, documentIds);
    }

    private void putIfPresent(Map<DocumentType, MultipartFile> map, DocumentType type, MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            map.put(type, file);
        }
    }
}
