package com.example.docsuriserver.document.service;

public record StoredFile(String fileKey, String contentType, long size) {
}
