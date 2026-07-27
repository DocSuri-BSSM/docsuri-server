package com.example.docsuriserver.controller;

import com.example.docsuriserver.common.ApiResponse;
import com.example.docsuriserver.dto.*;
import com.example.docsuriserver.service.DocumentParseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/document-sessions/{sessionId}")
public class DocumentParseController {

    private final DocumentParseService documentParseService;

    public DocumentParseController(DocumentParseService documentParseService) {
        this.documentParseService = documentParseService;
    }

    /** POST /document-sessions/{session_id}/parse — documentParseStart */
    @PostMapping("/parse")
    public ResponseEntity<ApiResponse<DocumentParseStartResponse>> documentParseStart(
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody DocumentParseStartRequest request) {

        DocumentParseStartResponse data = documentParseService.start(sessionId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.of("문서 파싱 작업 시작", data));
    }

    /** GET /document-sessions/{session_id}/parse-status — documentParseStatusSearch */
    @GetMapping("/parse-status")
    public ResponseEntity<ApiResponse<DocumentParseStatusSearchResponse>> documentParseStatusSearch(
            @PathVariable("sessionId") UUID sessionId) {

        return ResponseEntity.ok(
                ApiResponse.of("문서 파싱 상태 조회 성공", documentParseService.getStatus(sessionId)));
    }

    /** GET /document-sessions/{session_id}/extractions — documentExtractionListSearch */
    @GetMapping("/extractions")
    public ResponseEntity<ApiResponse<DocumentExtractionListSearchResponse>> documentExtractionListSearch(
            @PathVariable("sessionId") UUID sessionId) {

        return ResponseEntity.ok(
                ApiResponse.of("추출 데이터 조회 성공", documentParseService.getExtractions(sessionId)));
    }
}
