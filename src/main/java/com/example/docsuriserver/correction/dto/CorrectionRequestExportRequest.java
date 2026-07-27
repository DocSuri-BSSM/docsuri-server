package com.example.docsuriserver.correction.dto;

import com.example.docsuriserver.common.ExportFormat;
import jakarta.validation.constraints.NotNull;

public record CorrectionRequestExportRequest(
        @NotNull ExportFormat format,
        @NotNull Boolean includeValidationReport
) {
}
