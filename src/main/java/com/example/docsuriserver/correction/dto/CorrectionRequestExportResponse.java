package com.example.docsuriserver.correction.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CorrectionRequestExportResponse(
        UUID exportId,
        String fileName,
        String downloadUrl,
        LocalDateTime expiresAt
) {
}
