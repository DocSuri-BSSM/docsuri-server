package com.example.docsuriserver.guide.controller;

import com.example.docsuriserver.common.ApiResponse;
import com.example.docsuriserver.guide.dto.DisclaimerListSearchResponse;
import com.example.docsuriserver.guide.dto.TradeTermListSearchResponse;
import com.example.docsuriserver.guide.service.DisclaimerQueryService;
import com.example.docsuriserver.guide.service.GuideService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GuideController {

    private final GuideService guideService;
    private final DisclaimerQueryService disclaimerQueryService;

    public GuideController(GuideService guideService, DisclaimerQueryService disclaimerQueryService) {
        this.guideService = guideService;
        this.disclaimerQueryService = disclaimerQueryService;
    }

    /** GET /guides/trade-terms — tradeTermListSearch */
    @GetMapping("/guides/trade-terms")
    public ResponseEntity<ApiResponse<TradeTermListSearchResponse>> tradeTermListSearch(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.of("무역 용어 목록 조회 성공", guideService.searchTradeTerms(keyword)));
    }

    /** GET /guides/disclaimers — disclaimerListSearch */
    @GetMapping("/guides/disclaimers")
    public ResponseEntity<ApiResponse<DisclaimerListSearchResponse>> disclaimerListSearch() {
        return ResponseEntity.ok(ApiResponse.of("면책 문구 조회 성공", disclaimerQueryService.listActive()));
    }
}
