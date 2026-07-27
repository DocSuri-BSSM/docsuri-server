package com.example.docsuriserver.correction.controller;

import com.example.docsuriserver.common.ApiResponse;
import com.example.docsuriserver.correction.dto.CorrectionRequestCreateRequest;
import com.example.docsuriserver.correction.dto.CorrectionRequestCreateResponse;
import com.example.docsuriserver.correction.dto.CorrectionRequestExportRequest;
import com.example.docsuriserver.correction.dto.CorrectionRequestExportResponse;
import com.example.docsuriserver.correction.dto.CorrectionRequestSearchResponse;
import com.example.docsuriserver.correction.dto.CorrectionRequestUpdateRequest;
import com.example.docsuriserver.correction.service.CorrectionRequestExportService;
import com.example.docsuriserver.correction.service.CorrectionRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class CorrectionRequestController {

    private final CorrectionRequestService correctionRequestService;
    private final CorrectionRequestExportService exportService;

    public CorrectionRequestController(CorrectionRequestService correctionRequestService,
                                       CorrectionRequestExportService exportService) {
        this.correctionRequestService = correctionRequestService;
        this.exportService = exportService;
    }

    /** POST /document-sessions/{session_id}/correction-requests — correctionRequestCreate */
    @PostMapping("/document-sessions/{sessionId}/correction-requests")
    public ResponseEntity<ApiResponse<CorrectionRequestCreateResponse>> correctionRequestCreate(
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody CorrectionRequestCreateRequest request) {
        CorrectionRequestCreateResponse data = correctionRequestService.create(sessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of("B/L 수정 요청서 생성 성공", data));
    }

    /** GET /correction-requests/{correction_request_id} — correctionRequestSearch */
    @GetMapping("/correction-requests/{correctionRequestId}")
    public ResponseEntity<ApiResponse<CorrectionRequestSearchResponse>> correctionRequestSearch(
            @PathVariable("correctionRequestId") UUID correctionRequestId) {
        return ResponseEntity.ok(ApiResponse.of("B/L 수정 요청서 조회 성공", correctionRequestService.get(correctionRequestId)));
    }

    /** PATCH /correction-requests/{correction_request_id} — correctionRequestUpdate (204, ApiResponse 미사용) */
    @PatchMapping("/correction-requests/{correctionRequestId}")
    public ResponseEntity<Void> correctionRequestUpdate(
            @PathVariable("correctionRequestId") UUID correctionRequestId,
            @RequestBody CorrectionRequestUpdateRequest request) {
        correctionRequestService.update(correctionRequestId, request);
        return ResponseEntity.noContent().build();
    }

    /** POST /correction-requests/{correction_request_id}/export — correctionRequestExport */
    @PostMapping("/correction-requests/{correctionRequestId}/export")
    public ResponseEntity<ApiResponse<CorrectionRequestExportResponse>> correctionRequestExport(
            @PathVariable("correctionRequestId") UUID correctionRequestId,
            @Valid @RequestBody CorrectionRequestExportRequest request) {
        return ResponseEntity.ok(ApiResponse.of("파일 생성 성공", exportService.export(correctionRequestId, request)));
    }
}
