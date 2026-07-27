package com.example.docsuriserver.hscode.controller;

import com.example.docsuriserver.common.ApiResponse;
import com.example.docsuriserver.hscode.dto.HsCodeDetailSearchResponse;
import com.example.docsuriserver.hscode.dto.HsCodeJustificationCreateRequest;
import com.example.docsuriserver.hscode.dto.HsCodeJustificationCreateResponse;
import com.example.docsuriserver.hscode.dto.HsCodeRecommendationCreateRequest;
import com.example.docsuriserver.hscode.dto.HsCodeRecommendationCreateResponse;
import com.example.docsuriserver.hscode.service.HsCodeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class HsCodeController {

    private final HsCodeService hsCodeService;

    public HsCodeController(HsCodeService hsCodeService) {
        this.hsCodeService = hsCodeService;
    }

    /** POST /hs-code/recommendations — hsCodeRecommendationCreate */
    @PostMapping("/hs-code/recommendations")
    public ResponseEntity<ApiResponse<HsCodeRecommendationCreateResponse>> hsCodeRecommendationCreate(
            @Valid @RequestBody HsCodeRecommendationCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.of("HS Code 후보 추천 성공", hsCodeService.recommend(request)));
    }

    /** GET /hs-codes/{hs_code} — hsCodeDetailSearch (점(.) 포함 코드가 확장자로 잘리지 않게 :.+ 매핑) */
    @GetMapping("/hs-codes/{hsCode:.+}")
    public ResponseEntity<ApiResponse<HsCodeDetailSearchResponse>> hsCodeDetailSearch(
            @PathVariable("hsCode") String hsCode) {
        return ResponseEntity.ok(ApiResponse.of("HS Code 상세 조회 성공", hsCodeService.getDetail(hsCode)));
    }

    /** POST /hs-code/justifications — hsCodeJustificationCreate */
    @PostMapping("/hs-code/justifications")
    public ResponseEntity<ApiResponse<HsCodeJustificationCreateResponse>> hsCodeJustificationCreate(
            @Valid @RequestBody HsCodeJustificationCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.of("HS Code 소명 논리 생성 성공", hsCodeService.createJustification(request)));
    }
}
