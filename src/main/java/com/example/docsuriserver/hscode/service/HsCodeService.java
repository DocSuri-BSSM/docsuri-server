package com.example.docsuriserver.hscode.service;

import com.example.docsuriserver.ai.GeminiClient;
import com.example.docsuriserver.ai.GeminiModelTier;
import com.example.docsuriserver.ai.GeminiRequest;
import com.example.docsuriserver.ai.PromptTemplate;
import com.example.docsuriserver.ai.schema.HsCodeJustificationSchema;
import com.example.docsuriserver.ai.schema.HsCodeRecommendationSchema;
import com.example.docsuriserver.common.DisclaimerPosition;
import com.example.docsuriserver.common.ExternalApiException;
import com.example.docsuriserver.common.NotFoundException;
import com.example.docsuriserver.guide.service.DisclaimerQueryService;
import com.example.docsuriserver.hscode.dto.HsCodeDetailSearchResponse;
import com.example.docsuriserver.hscode.dto.HsCodeJustificationCreateRequest;
import com.example.docsuriserver.hscode.dto.HsCodeJustificationCreateResponse;
import com.example.docsuriserver.hscode.dto.HsCodeRecommendationCreateRequest;
import com.example.docsuriserver.hscode.dto.HsCodeRecommendationCreateResponse;
import com.example.docsuriserver.hscode.entity.HsCode;
import com.example.docsuriserver.hscode.entity.HsCodeCandidate;
import com.example.docsuriserver.hscode.entity.HsCodeJustification;
import com.example.docsuriserver.hscode.entity.HsCodeRecommendation;
import com.example.docsuriserver.hscode.repository.HsCodeJustificationRepository;
import com.example.docsuriserver.hscode.repository.HsCodeRecommendationRepository;
import com.example.docsuriserver.hscode.repository.HsCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HS Code는 생성하지 않고 선택한다 — hs_codes 테이블 후보 밖의 코드를 만들어내는 경로가 없어야 한다
 * (04-AI-INTEGRATION.md 4절).
 */
@Service
public class HsCodeService {

    private static final int CANDIDATE_SEARCH_LIMIT = 20;

    private static final String SYSTEM_INSTRUCTION_RECOMMEND =
            "당신은 관세청 출신의 베테랑 관세사입니다. 주어진 HS Code 후보 목록 안에서만 순위와 근거를 판단합니다.";
    private static final String SYSTEM_INSTRUCTION_JUSTIFICATION =
            "당신은 관세청 출신의 베테랑 관세사입니다. 세관에 제출할 수 있는 정교한 소명 논리를 작성합니다.";

    private static final List<String> LEGAL_BASIS_CANDIDATES = List.of(
            "관세율표 해석에 관한 통칙 제1호",
            "관세율표 해석에 관한 통칙 제2호",
            "관세율표 해석에 관한 통칙 제3호",
            "관세율표 해석에 관한 통칙 제4호",
            "관세율표 해석에 관한 통칙 제5호",
            "관세율표 해석에 관한 통칙 제6호");

    private final HsCodeRepository hsCodeRepository;
    private final HsCodeRecommendationRepository recommendationRepository;
    private final HsCodeJustificationRepository justificationRepository;
    private final GeminiClient geminiClient;
    private final PromptTemplate promptTemplate;
    private final DisclaimerQueryService disclaimerQueryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HsCodeService(HsCodeRepository hsCodeRepository,
                         HsCodeRecommendationRepository recommendationRepository,
                         HsCodeJustificationRepository justificationRepository,
                         GeminiClient geminiClient,
                         PromptTemplate promptTemplate,
                         DisclaimerQueryService disclaimerQueryService) {
        this.hsCodeRepository = hsCodeRepository;
        this.recommendationRepository = recommendationRepository;
        this.justificationRepository = justificationRepository;
        this.geminiClient = geminiClient;
        this.promptTemplate = promptTemplate;
        this.disclaimerQueryService = disclaimerQueryService;
    }

    @Transactional
    public HsCodeRecommendationCreateResponse recommend(HsCodeRecommendationCreateRequest request) {
        String keyword = (request.productName() + " " + request.productDescription()).trim();
        List<HsCode> candidates = hsCodeRepository.searchCandidates(keyword, CANDIDATE_SEARCH_LIMIT);

        List<HsCodeCandidate> ranked = candidates.isEmpty() ? List.of() : rankCandidates(request, candidates);
        List<HsCodeCandidate> limited = ranked.stream().limit(request.maxCandidates()).toList();

        HsCodeRecommendation saved = recommendationRepository.save(HsCodeRecommendation.create(
                request.productName(), request.productDescription(), request.originCountryCode(),
                request.maxCandidates(), request.outputLanguage(), limited));

        String disclaimer = disclaimerQueryService.getContent(DisclaimerPosition.HS_CODE);
        return HsCodeRecommendationCreateResponse.of(saved.getRecommendationId(), limited, disclaimer);
    }

    private List<HsCodeCandidate> rankCandidates(HsCodeRecommendationCreateRequest request, List<HsCode> candidates) {
        Map<String, HsCode> byCode = candidates.stream().collect(Collectors.toMap(HsCode::getHsCode, c -> c));

        String userPrompt = promptTemplate.render("hs-code-recommendation.md", Map.of(
                "PRODUCT_NAME", request.productName(),
                "PRODUCT_DESCRIPTION", request.productDescription(),
                "ORIGIN_COUNTRY_CODE", request.originCountryCode() == null ? "" : request.originCountryCode(),
                "HS_CODE_CANDIDATES_JSON", buildCandidatesJson(candidates),
                "OUTPUT_LANGUAGE", request.outputLanguage()));

        GeminiRequest geminiRequest = new GeminiRequest(
                SYSTEM_INSTRUCTION_RECOMMEND, userPrompt, HsCodeRecommendationSchema.schema(), 0.1, GeminiModelTier.REASONING);
        JsonNode result = geminiClient.generate(geminiRequest);

        List<HsCodeCandidate> ranked = new ArrayList<>();
        JsonNode items = result.path("candidates");
        if (items.isArray()) {
            int fallbackRank = 1;
            for (JsonNode node : items) {
                String hsCode = node.path("hs_code").asString(null);
                HsCode dbCode = hsCode == null ? null : byCode.get(hsCode);
                if (dbCode == null) {
                    continue; // 후보 목록 밖의 코드는 폐기한다
                }
                int rank = node.path("rank").asInt(fallbackRank++);
                BigDecimal confidence = node.path("confidence").decimalValue(BigDecimal.ZERO);
                String reason = node.path("reason").asString("");
                // korean_name/english_name은 LLM 생성값을 쓰지 않고 DB 값으로 덮어쓴다
                ranked.add(new HsCodeCandidate(rank, dbCode.getHsCode(), dbCode.getKoreanName(), dbCode.getEnglishName(), confidence, reason));
            }
        }
        ranked.sort(Comparator.comparingInt(HsCodeCandidate::rank));
        return ranked;
    }

    private String buildCandidatesJson(List<HsCode> candidates) {
        List<Map<String, Object>> list = candidates.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("hs_code", c.getHsCode());
            m.put("korean_name", c.getKoreanName());
            m.put("english_name", c.getEnglishName());
            m.put("description", c.getDescription());
            return m;
        }).toList();
        return writeJson(list);
    }

    @Transactional(readOnly = true)
    public HsCodeDetailSearchResponse getDetail(String hsCode) {
        HsCode entity = hsCodeRepository.findById(hsCode)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 HS Code입니다."));
        return HsCodeDetailSearchResponse.from(entity);
    }

    @Transactional
    public HsCodeJustificationCreateResponse createJustification(HsCodeJustificationCreateRequest request) {
        HsCode hsCode = hsCodeRepository.findById(request.hsCode())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 HS Code입니다."));

        String userPrompt = promptTemplate.render("hs-code-justification.md", Map.of(
                "HS_CODE", request.hsCode(),
                "OFFICIAL_NAME", hsCode.getKoreanName(),
                "PRODUCT_NAME", request.productName(),
                "PRODUCT_DESCRIPTION", request.productDescription(),
                "ADDITIONAL_FACTS_JSON", writeJson(request.additionalFacts()),
                "LEGAL_BASIS_CANDIDATES_JSON", writeJson(LEGAL_BASIS_CANDIDATES),
                "OUTPUT_LANGUAGE", request.outputLanguage()));

        GeminiRequest geminiRequest = new GeminiRequest(
                SYSTEM_INSTRUCTION_JUSTIFICATION, userPrompt, HsCodeJustificationSchema.schema(), 0.3, GeminiModelTier.REASONING);
        JsonNode result = geminiClient.generate(geminiRequest);

        String title = truncate(result.path("title").asString(""), 100);
        String content = truncate(result.path("content").asString(""), 10000);

        List<String> legalBasis = new ArrayList<>();
        JsonNode basisNode = result.path("legal_basis");
        if (basisNode.isArray()) {
            for (JsonNode node : basisNode) {
                String value = node.asString(null);
                if (value != null && LEGAL_BASIS_CANDIDATES.contains(value)) {
                    legalBasis.add(value);
                }
            }
        }

        HsCodeJustification saved = justificationRepository.save(HsCodeJustification.create(
                request.hsCode(), request.productName(), request.productDescription(), request.additionalFacts(),
                request.outputLanguage(), title, content, legalBasis));

        String disclaimer = disclaimerQueryService.getContent(DisclaimerPosition.HS_CODE);
        return new HsCodeJustificationCreateResponse(saved.getJustificationId(), title, content, legalBasis, disclaimer);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new ExternalApiException("JSON 직렬화 실패", e);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
