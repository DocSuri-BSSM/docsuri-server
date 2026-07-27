package com.example.docsuriserver.hscode.dto;

import java.util.List;
import java.util.UUID;

public record HsCodeJustificationCreateResponse(
        UUID justificationId,
        String title,
        String content,
        List<String> legalBasis,
        String disclaimer
) {
}
