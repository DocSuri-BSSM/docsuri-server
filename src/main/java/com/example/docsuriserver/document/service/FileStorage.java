package com.example.docsuriserver.document.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 저장소 교체(로컬 -> S3)를 대비한 추상화. 서비스 코드가 로컬 경로를 직접 다루면 안 된다.
 */
public interface FileStorage {

    StoredFile store(String directory, MultipartFile file);

    StoredFile store(String directory, byte[] content, String fileName, String contentType);

    byte[] load(String fileKey);

    void delete(String fileKey);
}
