package com.example.docsuriserver.dto;

import java.util.List;
import java.util.UUID;

public record DocumentUploadResponse(
        UUID sessionId,
        List<UUID> documentIds
) {
}
