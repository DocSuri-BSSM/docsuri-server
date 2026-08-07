package com.example.docsuriserver.correction.service;

import com.example.docsuriserver.ai.GeminiClient;
import com.example.docsuriserver.ai.GeminiModelTier;
import com.example.docsuriserver.ai.GeminiRequest;
import com.example.docsuriserver.ai.schema.CorrectionRequestSchema;
import com.example.docsuriserver.common.ConflictException;
import com.example.docsuriserver.common.CorrectionStatus;
import com.example.docsuriserver.common.InvalidRequestException;
import com.example.docsuriserver.common.IssueStatus;
import com.example.docsuriserver.common.NotFoundException;
import com.example.docsuriserver.ai.PromptTemplate;
import com.example.docsuriserver.correction.dto.CorrectionRequestCreateRequest;
import com.example.docsuriserver.correction.dto.CorrectionRequestCreateResponse;
import com.example.docsuriserver.correction.dto.CorrectionRequestSearchResponse;
import com.example.docsuriserver.correction.dto.CorrectionRequestUpdateRequest;
import com.example.docsuriserver.correction.entity.CorrectionRequest;
import com.example.docsuriserver.correction.entity.CorrectionVariable;
import com.example.docsuriserver.correction.repository.CorrectionRequestRepository;
import com.example.docsuriserver.document.service.DocumentSessionQueryService;
import com.example.docsuriserver.validation.entity.ValidationIssue;
import com.example.docsuriserver.validation.entity.ValidationRun;
import com.example.docsuriserver.validation.service.ValidationQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CorrectionRequestService {

    private static final String SYSTEM_INSTRUCTION = """
            당신은 중소기업의 무역 행정 업무를 완벽하게 보좌하는 전문 AI 무역 비서입니다. \
            포워더·선사 담당자가 군말 없이 수정해 줄 수 있도록 정중하고 명확한 정정 요청서 초안을 작성합니다.""";

    private final CorrectionRequestRepository repository;
    private final DocumentSessionQueryService sessionQueryService;
    private final ValidationQueryService validationQueryService;
    private final GeminiClient geminiClient;
    private final PromptTemplate promptTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CorrectionRequestService(CorrectionRequestRepository repository,
                                    DocumentSessionQueryService sessionQueryService,
                                    ValidationQueryService validationQueryService,
                                    GeminiClient geminiClient,
                                    PromptTemplate promptTemplate) {
        this.repository = repository;
        this.sessionQueryService = sessionQueryService;
        this.validationQueryService = validationQueryService;
        this.geminiClient = geminiClient;
        this.promptTemplate = promptTemplate;
    }

    @Transactional
    public CorrectionRequestCreateResponse create(UUID sessionId, CorrectionRequestCreateRequest request) {
        sessionQueryService.requireExists(sessionId);
        ValidationRun run = validationQueryService.getCompletedRun(request.validationRunId(), sessionId);

        List<ValidationIssue> errorIssues = run.getIssues() == null ? List.of() : run.getIssues().stream()
                .filter(issue -> issue.status() == IssueStatus.ERROR)
                .toList();
        if (errorIssues.isEmpty()) {
            throw new InvalidRequestException("정정할 오류가 없습니다.");
        }

        String userPrompt = promptTemplate.render("correction-request.md", Map.of(
                "ERROR_ISSUES_JSON", writeJson(errorIssues),
                "OUTPUT_LANGUAGE", request.outputLanguage(),
                "ADDITIONAL_INSTRUCTION", request.additionalInstruction() == null ? "없음" : request.additionalInstruction()));

        GeminiRequest geminiRequest = new GeminiRequest(
                SYSTEM_INSTRUCTION, userPrompt, CorrectionRequestSchema.schema(), 0.2, GeminiModelTier.FAST);
        JsonNode result = geminiClient.generate(geminiRequest);

        String title = result.path("title").asString("");
        String content = result.path("content").asString("");
        List<CorrectionVariable> variables = parseVariables(result.path("variables"));

        CorrectionRequest saved = repository.save(CorrectionRequest.create(
                sessionId, request.validationRunId(), request.outputLanguage(), request.additionalInstruction(),
                title, content, variables));

        return new CorrectionRequestCreateResponse(saved.getCorrectionRequestId());
    }

    private List<CorrectionVariable> parseVariables(JsonNode node) {
        List<CorrectionVariable> variables = new ArrayList<>();
        Set<String> usedKeys = new HashSet<>();
        if (node.isArray()) {
            for (JsonNode v : node) {
                String key = v.path("variable_key").asString(null);
                if (key == null || key.isBlank()) {
                    continue;
                }
                // variable_key 중복 방지: 이미 쓴 키면 _2, _3... 접미사를 붙여 유일하게 만든다.
                String uniqueKey = key;
                int suffix = 2;
                while (!usedKeys.add(uniqueKey)) {
                    uniqueKey = key + "_" + suffix++;
                }
                variables.add(new CorrectionVariable(
                        uniqueKey,
                        v.path("label").asString(""),
                        v.path("value").asString(""),
                        v.path("required").asBoolean(true)));
            }
        }
        return variables;
    }

    @Transactional(readOnly = true)
    public CorrectionRequestSearchResponse get(UUID correctionRequestId) {
        return CorrectionRequestSearchResponse.from(findOrThrow(correctionRequestId));
    }

    @Transactional
    public void update(UUID correctionRequestId, CorrectionRequestUpdateRequest request) {
        CorrectionRequest r = findOrThrow(correctionRequestId);
        if (r.getStatus() == CorrectionStatus.EXPORTED) {
            throw new ConflictException("이미 내보내기 완료된 요청서는 수정할 수 없습니다.");
        }
        if (request.title() == null && request.content() == null && request.variables() == null) {
            throw new InvalidRequestException("최소 하나의 필드는 있어야 합니다.");
        }

        String content = request.content() != null ? request.content() : r.getContent();

        if (request.variables() != null) {
            List<CorrectionVariable> existing = r.getVariables() == null ? List.of() : r.getVariables();
            validateVariableKeys(existing, request.variables());
            content = substitutePlaceholders(content, existing, request.variables());
            r.updateVariables(mergeVariables(existing, request.variables()));
        }

        r.updateContent(content);
        if (request.title() != null) {
            r.updateTitle(request.title());
        }
        r.refreshConfirmationStatus();
    }

    private void validateVariableKeys(List<CorrectionVariable> existing, List<CorrectionRequestUpdateRequest.VariableUpdate> updates) {
        Set<String> existingKeys = new HashSet<>();
        existing.forEach(v -> existingKeys.add(v.variableKey()));
        for (CorrectionRequestUpdateRequest.VariableUpdate update : updates) {
            if (!existingKeys.contains(update.variableKey())) {
                throw new InvalidRequestException("존재하지 않는 variable_key입니다: " + update.variableKey());
            }
        }
    }

    private String substitutePlaceholders(String content, List<CorrectionVariable> existing,
                                          List<CorrectionRequestUpdateRequest.VariableUpdate> updates) {
        Map<String, String> oldValueByKey = new HashMap<>();
        existing.forEach(v -> oldValueByKey.put(v.variableKey(), v.value()));

        String result = content;
        for (CorrectionRequestUpdateRequest.VariableUpdate update : updates) {
            String oldValue = oldValueByKey.get(update.variableKey());
            if (oldValue != null && update.value() != null && result != null) {
                result = result.replace(oldValue, update.value());
            }
        }
        return result;
    }

    private List<CorrectionVariable> mergeVariables(List<CorrectionVariable> existing,
                                                     List<CorrectionRequestUpdateRequest.VariableUpdate> updates) {
        Map<String, String> updateMap = new HashMap<>();
        updates.forEach(u -> updateMap.put(u.variableKey(), u.value()));

        return existing.stream()
                .map(v -> updateMap.containsKey(v.variableKey())
                        ? new CorrectionVariable(v.variableKey(), v.label(), updateMap.get(v.variableKey()), v.required())
                        : v)
                .toList();
    }

    private CorrectionRequest findOrThrow(UUID correctionRequestId) {
        return repository.findById(correctionRequestId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 정정 요청서입니다."));
    }

    private String writeJson(Object value) {
        return objectMapper.writeValueAsString(value);
    }
}
