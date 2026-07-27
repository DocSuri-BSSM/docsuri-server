package com.example.docsuriserver.document.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
public class LocalFileStorage implements FileStorage {

    private final Path root;

    public LocalFileStorage(@Value("${app.storage.local-path:./storage}") String rootPath) {
        this.root = Path.of(rootPath);
    }

    @Override
    public StoredFile store(String directory, MultipartFile file) {
        try {
            String originalFileName = file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename();
            String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
            return storeInternal(directory, file.getBytes(), originalFileName, contentType);
        } catch (IOException e) {
            throw new UncheckedIOException("파일 저장 실패: " + file.getOriginalFilename(), e);
        }
    }

    @Override
    public StoredFile store(String directory, byte[] content, String fileName, String contentType) {
        return storeInternal(directory, content, fileName, contentType);
    }

    private StoredFile storeInternal(String directory, byte[] content, String fileName, String contentType) {
        try {
            Path dir = root.resolve(directory);
            Files.createDirectories(dir);

            String sanitized = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
            String storedName = UUID.randomUUID() + "_" + sanitized;
            Files.write(dir.resolve(storedName), content);

            String fileKey = directory + "/" + storedName;
            return new StoredFile(fileKey, contentType, content.length);
        } catch (IOException e) {
            throw new UncheckedIOException("파일 저장 실패: " + fileName, e);
        }
    }

    @Override
    public byte[] load(String fileKey) {
        try {
            return Files.readAllBytes(root.resolve(fileKey));
        } catch (IOException e) {
            throw new UncheckedIOException("파일 읽기 실패: " + fileKey, e);
        }
    }

    @Override
    public void delete(String fileKey) {
        try {
            Files.deleteIfExists(root.resolve(fileKey));
        } catch (IOException e) {
            throw new UncheckedIOException("파일 삭제 실패: " + fileKey, e);
        }
    }
}
