package com.example.docsuriserver.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * 저장소 교체(로컬 -> S3)를 대비한 추상화.
 */
public interface FileStorage {

    /** 저장 후 접근 가능한 URL(또는 경로)을 반환 */
    String store(UUID sessionId, MultipartFile file);
}
