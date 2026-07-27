package com.example.docsuriserver.validation.service;

import com.example.docsuriserver.ai.GeminiClient;
import com.example.docsuriserver.ai.GeminiModelTier;
import com.example.docsuriserver.ai.GeminiRequest;
import com.example.docsuriserver.ai.PromptTemplate;
import com.example.docsuriserver.ai.schema.ValidationNarrativeSchema;
import com.example.docsuriserver.common.DisclaimerPosition;
import com.example.docsuriserver.common.IssueStatus;
import com.example.docsuriserver.common.OverallSignal;
import com.example.docsuriserver.common.ValidationRule;
import com.example.docsuriserver.document.entity.ExtractedDocument;
import com.example.docsuriserver.document.service.DocumentParseQueryService;
import com.example.docsuriserver.guide.service.DisclaimerQueryService;
import com.example.docsuriserver.validation.entity.ValidationIssue;
import com.example.docsuriserver.validation.entity.ValidationRun;
import com.example.docsuriserver.validation.repository.ValidationRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Java(ValidationEngine)가 판정을 끝낸 뒤, LLM에는 서술문(title/subtitle/cause/riskWarning) 생성만 맡긴다.
 * LLM 응답의 rule/status는 신뢰하지 않고, Java가 만든 rule 매칭으로만 조인한다.
 */
@Component
public class ValidationWorker {

    private static final Logger log = LoggerFactory.getLogger(ValidationWorker.class);

    private static final String SYSTEM_INSTRUCTION = """
            당신은 20년 경력의 관세사입니다. 이미 계산되고 판정된 무역서류 교차검증 결과에 대한 \
            설명 문장만 작성하며, 절대 숫자를 재계산하거나 등급을 바꾸지 않습니다.""";

    private static final Comparator<ValidationIssue> ISSUE_ORDER = Comparator
            .comparingInt((ValidationIssue vi) -> switch (vi.status()) {
                case ERROR -> 0;
                case WARNING -> 1;
                case NORMAL -> 2;
            })
            .thenComparingInt(vi -> vi.rule().ordinal());

    private final ValidationRunRepository runRepository;
    private final DocumentParseQueryService parseQueryService;
    private final ValidationEngine engine;
    private final GeminiClient geminiClient;
    private final PromptTemplate promptTemplate;
    private final DisclaimerQueryService disclaimerQueryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ValidationWorker(ValidationRunRepository runRepository,
                            DocumentParseQueryService parseQueryService,
                            ValidationEngine engine,
                            GeminiClient geminiClient,
                            PromptTemplate promptTemplate,
                            DisclaimerQueryService disclaimerQueryService) {
        this.runRepository = runRepository;
        this.parseQueryService = parseQueryService;
        this.engine = engine;
        this.geminiClient = geminiClient;
        this.promptTemplate = promptTemplate;
        this.disclaimerQueryService = disclaimerQueryService;
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ValidationRunCreatedEvent event) {
        run(event.validationRunId());
    }

    private void run(UUID validationRunId) {
        ValidationRun run = runRepository.findById(validationRunId).orElseThrow();
        try {
            run.start();
            runRepository.save(run);

            List<ExtractedDocument> documents = parseQueryService.getCompletedExtractions(run.getSessionId());
            List<Finding> findings = engine.evaluate(documents, run.getRules(), run.getWeightTolerancePercent());

            Map<ValidationRule, NarrativeText> narratives = findings.isEmpty() ? Map.of() : generateNarratives(findings);

            List<ValidationIssue> issues = findings.stream()
                    .map(f -> toIssue(f, narratives.get(f.rule())))
                    .sorted(ISSUE_ORDER)
                    .toList();

            int total = issues.size();
            int normal = (int) issues.stream().filter(i -> i.status() == IssueStatus.NORMAL).count();
            int warning = (int) issues.stream().filter(i -> i.status() == IssueStatus.WARNING).count();
            int error = (int) issues.stream().filter(i -> i.status() == IssueStatus.ERROR).count();
            if (normal + warning + error != total) {
                throw new IllegalStateException("카운트 불변식이 깨졌습니다: normal+warning+error != total_checked");
            }

            OverallSignal signal = error > 0 ? OverallSignal.RED : warning > 0 ? OverallSignal.YELLOW : OverallSignal.GREEN;
            String disclaimer = disclaimerQueryService.getContent(DisclaimerPosition.VALIDATION);

            run.complete(signal, total, normal, warning, error, issues, disclaimer);
            runRepository.save(run);
        } catch (Exception e) {
            log.error("교차검증 실패 validationRunId={}", validationRunId, e);
            run.fail("교차검증 중 오류가 발생했습니다.");
            runRepository.save(run);
        }
    }

    private Map<ValidationRule, NarrativeText> generateNarratives(List<Finding> findings) {
        String findingsJson = buildFindingsJson(findings);
        String userPrompt = promptTemplate.render("validation-narrative.md", Map.of("FINDINGS_JSON", findingsJson));
        GeminiRequest request = new GeminiRequest(
                SYSTEM_INSTRUCTION, userPrompt, ValidationNarrativeSchema.schema(), 0.1, GeminiModelTier.FAST);

        JsonNode result = geminiClient.generate(request);
        Map<ValidationRule, NarrativeText> narratives = new HashMap<>();
        JsonNode items = result.path("narratives");
        if (items.isArray()) {
            for (JsonNode node : items) {
                ValidationRule rule = parseRule(node.path("rule").asString(null));
                if (rule == null) {
                    continue; // 04-AI-INTEGRATION.md 5절: rule 값이 Enum에 없으면 폐기
                }
                narratives.put(rule, new NarrativeText(
                        truncate(node.path("title").asString(""), 100),
                        node.path("subtitle").asString(""),
                        truncate(node.path("cause").asString(""), 10000),
                        node.path("risk_warning").asString("")));
            }
        }
        return narratives;
    }

    private String buildFindingsJson(List<Finding> findings) {
        List<Map<String, Object>> list = findings.stream().map(f -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rule", f.rule().name());
            m.put("status", f.status().name());
            m.put("values", f.values().stream()
                    .map(v -> Map.of(
                            "doc_type", v.docType(),
                            "value", v.value() == null ? "" : v.value(),
                            "unit", v.unit() == null ? "" : v.unit()))
                    .toList());
            if (f.diffPercent() != null) {
                m.put("diff_percent", f.diffPercent());
            }
            if (f.tolerancePercent() != null) {
                m.put("tolerance_percent", f.tolerancePercent());
            }
            return m;
        }).toList();

        return objectMapper.writeValueAsString(list);
    }

    private ValidationIssue toIssue(Finding f, NarrativeText n) {
        if (n == null) {
            return new ValidationIssue(f.rule(), f.status(), f.rule().name(), "", f.comparisons(), "", "");
        }
        return new ValidationIssue(f.rule(), f.status(), n.title(), n.subtitle(), f.comparisons(), n.cause(), n.riskWarning());
    }

    private ValidationRule parseRule(String name) {
        if (name == null) {
            return null;
        }
        try {
            return ValidationRule.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
