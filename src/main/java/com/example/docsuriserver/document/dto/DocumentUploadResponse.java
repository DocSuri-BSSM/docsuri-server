package com.example.docsuriserver.document.dto;

import java.util.List;
import java.util.UUID;

public record DocumentUploadResponse(
        UUID sessionId,
        List<UUID> documentIds
) {
}
