package com.example.docsuriserver.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class LocalFileStorage implements FileStorage {

    private final Path root;

    public LocalFileStorage(@Value("${app.storage.local-path:./uploads}") String rootPath) {
        this.root = Path.of(rootPath);
    }

    @Override
    public String store(UUID sessionId, MultipartFile file) {
        try {
            Path dir = root.resolve(sessionId.toString());
            Files.createDirectories(dir);

            String original = file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename();
            String storedName = UUID.randomUUID() + "_" + original.replaceAll("[^A-Za-z0-9._-]", "_");
            Path target = dir.resolve(storedName);

            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/files/" + sessionId + "/" + storedName;
        } catch (IOException e) {
            throw new UncheckedIOException("파일 저장 실패: " + file.getOriginalFilename(), e);
        }
    }
}
