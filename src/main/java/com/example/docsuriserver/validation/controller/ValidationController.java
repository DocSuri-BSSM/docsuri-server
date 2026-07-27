package com.example.docsuriserver.validation.controller;

import com.example.docsuriserver.common.ApiResponse;
import com.example.docsuriserver.validation.dto.ValidationCreateRequest;
import com.example.docsuriserver.validation.dto.ValidationCreateResponse;
import com.example.docsuriserver.validation.dto.ValidationResultSearchResponse;
import com.example.docsuriserver.validation.service.ValidationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/document-sessions/{sessionId}")
public class ValidationController {

    private final ValidationService validationService;

    public ValidationController(ValidationService validationService) {
        this.validationService = validationService;
    }

    /** POST /document-sessions/{session_id}/validation — validationCreate */
    @PostMapping("/validation")
    public ResponseEntity<ApiResponse<ValidationCreateResponse>> validationCreate(
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody ValidationCreateRequest request) {

        ValidationCreateResponse data = validationService.start(sessionId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.of("교차검증 작업 시작", data));
    }

    /** GET /document-sessions/{session_id}/validation-result — validationResultSearch */
    @GetMapping("/validation-result")
    public ResponseEntity<ApiResponse<ValidationResultSearchResponse>> validationResultSearch(
            @PathVariable("sessionId") UUID sessionId) {

        return ResponseEntity.ok(ApiResponse.of("교차검증 결과 조회 성공", validationService.getResult(sessionId)));
    }
}
