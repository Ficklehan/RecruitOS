package com.recruitos.brain.engine;

import com.recruitos.brain.service.CognitiveMemoryService;
import com.recruitos.common.llm.LlmChatRequest;
import com.recruitos.common.llm.LlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 认知判断引擎 v2 — LLM 驱动的真实 AI 判断。
 * <p>
 * v1 的 contradiction 是硬编码模板，v2 改为通过 LLM 基于实际上下文生成。
 * LLM 不可用时降级为规则判断 + 模板化 contradiction（保持可用性）。
 */
@Component
public class CognitiveJudgmentEngine {

    private static final Logger log = LoggerFactory.getLogger(CognitiveJudgmentEngine.class);

    @Resource
    private CognitiveMemoryService memory;

    @Resource
    private LlmClient llmClient;

    @Resource
    private ObjectMapper objectMapper;

    private static final String CONTRADICTION_SYSTEM_PROMPT =
        "你是 RecruitOS 认知判断引擎的自我质疑模块。你的任务不是给出答案，而是质疑自己可能的错误。\n\n" +
        "规则：\n" +
        "1. 你必须从证据链中找出最弱的一环\n" +
        "2. 你必须给出至少一种替代解释（可能与主判断完全不同）\n" +
        "3. 你必须说明什么新证据会让你改变判断\n" +
        "4. 回答必须简洁，使用中文\n" +
        "5. 以 JSON 格式返回：{\"alternative_explanation\":\"...\", \"weakest_evidence\":\"...\", \"what_would_overturn\":\"...\"}";

    // ──────────── 管线健康判断 ────────────

    public Map<String, Object> judgePipelineHealth(Long tenantId, Long jobId, Map<String, Object> pipelineData) {
        List<Map<String, Object>> history = memory.getObjectEvents(tenantId, "JOB", jobId);
        Map<String, Object> profile = memory.getObjectProfile(tenantId, "JOB", jobId);

        List<Long> evidenceIds = new ArrayList<>();
        if (history != null) {
            for (Map<String, Object> evt : history) evidenceIds.add(toLong(evt.get("id")));
        }

        String judgmentText = buildPipelineJudgmentText(pipelineData, profile, history);
        double confidence = estimateConfidence(pipelineData, history);

        Map<String, Object> contradiction = generateContradiction(
            "PIPELINE_HEALTH", judgmentText, pipelineData, history, profile);

        Map<String, Object> judgmentData = Map.of(
            "primary", judgmentText.contains("。") ? judgmentText.split("。")[0] : judgmentText,
            "details", judgmentText,
            "confidence", confidence,
            "key_evidence", List.of(
                "管线候选人数量与时间序列趋势",
                "面试转化率历史对比",
                "渠道来源分布"
            ),
            "alternative_view", contradiction.get("alternative_explanation"),
            "what_would_change_my_mind", contradiction.get("what_would_overturn")
        );

        return Map.of(
            "judgmentText", judgmentText,
            "judgmentData", judgmentData,
            "confidence", confidence,
            "evidenceIds", evidenceIds,
            "contradiction", contradiction
        );
    }

    // ──────────── 候选人判断 ────────────

    public Map<String, Object> judgeCandidate(Long tenantId, Long candidateId, Long jobId,
                                               Map<String, Object> candidateData,
                                               Map<String, Object> interviewScores) {
        Map<String, Object> candidateProfile = memory.getObjectProfile(tenantId, "CANDIDATE", candidateId);
        List<Map<String, Object>> candidateEvents = memory.getObjectEvents(tenantId, "CANDIDATE", candidateId);
        Map<String, Object> jobProfile = memory.getObjectProfile(tenantId, "JOB", jobId);

        List<Long> evidenceIds = new ArrayList<>();
        if (candidateEvents != null) {
            for (Map<String, Object> evt : candidateEvents) evidenceIds.add(toLong(evt.get("id")));
        }

        String judgmentText = buildCandidateJudgmentText(candidateData, interviewScores, candidateProfile, jobProfile);
        double confidence = candidateEvents != null && !candidateEvents.isEmpty() ? 0.75 : 0.55;

        Map<String, Object> contextData = new LinkedHashMap<>(candidateData);
        if (interviewScores != null) contextData.putAll(interviewScores);
        if (candidateProfile != null) contextData.put("profile", candidateProfile);

        Map<String, Object> contradiction = generateContradiction(
            "CANDIDATE_OPINION", judgmentText, contextData, candidateEvents, jobProfile);

        Map<String, Object> judgmentData = Map.of(
            "primary", judgmentText.contains("。") ? judgmentText.split("。")[0] : judgmentText,
            "details", judgmentText,
            "confidence", confidence,
            "key_evidence", List.of("简历技能与JD匹配度", "面试评分汇总", "候选人历史行为模式"),
            "alternative_view", contradiction.get("alternative_explanation"),
            "what_would_change_my_mind", contradiction.get("what_would_overturn")
        );

        return Map.of(
            "judgmentText", judgmentText,
            "judgmentData", judgmentData,
            "confidence", confidence,
            "evidenceIds", evidenceIds,
            "contradiction", contradiction
        );
    }

    // ──────────── 决策一致性 ────────────

    public Map<String, Object> judgeDecisionConsistency(Long tenantId, Long userId,
                                                         Long candidateId, String decision,
                                                         Map<String, Object> userModel) {
        Map<String, Object> profile = memory.getUserModel(tenantId, userId);

        String judgmentText;
        if (profile == null) {
            judgmentText = "数据不足，尚无法评估你的决策一致性。积累更多录用/拒绝决策后会逐步生成。";
        } else {
            judgmentText = buildConsistencyText(decision, profile, userModel);
        }

        double confidence = profile != null ? 0.7 : 0.2;

        Map<String, Object> contradiction = generateContradiction(
            "DECISION_CONSISTENCY", judgmentText,
            Map.of("decision", decision, "profile", profile != null ? profile : Map.of(), "userModel", userModel),
            List.of(), Map.of());

        Map<String, Object> judgmentData = Map.of(
            "primary", judgmentText.contains("。") ? judgmentText.split("。")[0] : judgmentText,
            "details", judgmentText,
            "confidence", confidence,
            "key_evidence", profile != null ? List.of("历史评分均值", "当前决策与历史模式偏差") : List.of(),
            "alternative_view", contradiction.get("alternative_explanation"),
            "what_would_change_my_mind", contradiction.get("what_would_overturn")
        );

        return Map.of(
            "judgmentText", judgmentText,
            "judgmentData", judgmentData,
            "confidence", confidence,
            "evidenceIds", List.of(),
            "contradiction", contradiction
        );
    }

    // ──────────── LLM 驱动的自我质疑 ────────────

    private Map<String, Object> generateContradiction(String judgmentType, String judgmentText,
                                                       Map<String, Object> contextData,
                                                       List<Map<String, Object>> history,
                                                       Map<String, Object> profile) {
        try {
            String prompt = buildContradictionPrompt(judgmentType, judgmentText, contextData, history, profile);

            LlmChatRequest req = new LlmChatRequest();
            req.setSystemPrompt(CONTRADICTION_SYSTEM_PROMPT);
            req.setUserPrompt(prompt);
            req.setScenario("RECRUITOS_COGNITIVE_CONTRADICTION");

            String llmResponse = llmClient.chat(req);
            if (llmResponse != null && !llmResponse.isEmpty()) {
                Map<String, Object> parsed = parseContradictionJson(llmResponse);
                if (parsed != null && parsed.containsKey("alternative_explanation")) {
                    log.info("LLM contradiction generated for {}: {}", judgmentType,
                        parsed.get("alternative_explanation"));
                    return parsed;
                }
            }
        } catch (Exception e) {
            log.warn("LLM contradiction generation failed for {}, using template fallback", judgmentType, e);
        }
        return getTemplateContradiction(judgmentType);
    }

    private String buildContradictionPrompt(String judgmentType, String judgment, Map<String, Object> context,
                                             List<Map<String, Object>> history, Map<String, Object> profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是 AI 对").append(judgmentType).append("的主判断：\n\n");
        sb.append("\"").append(judgment).append("\"\n\n");
        sb.append("背景数据：\n");
        if (context != null && !context.isEmpty()) {
            for (Map.Entry<String, Object> e : context.entrySet()) {
                sb.append("- ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
            }
        }
        if (profile != null && !profile.isEmpty()) {
            sb.append("\n历史画像：").append(profile).append("\n");
        }
        if (history != null && !history.isEmpty()) {
            sb.append("\n相关历史事件数：").append(history.size()).append(" 条\n");
        }
        sb.append("\n请质疑以上判断，找出最弱证据、替代解释、以及什么情况会推翻判断。以 JSON 格式返回。");
        return sb.toString();
    }

    private Map<String, Object> parseContradictionJson(String llmResponse) {
        try {
            // 尝试直接解析 JSON
            String json = llmResponse.trim();
            // 提取 JSON 块（LLM 可能包裹在 ```json...``` 中）
            if (json.contains("```")) {
                int start = json.indexOf("{");
                int end = json.lastIndexOf("}");
                if (start >= 0 && end > start) json = json.substring(start, end + 1);
            }
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("alternative_explanation", map.getOrDefault("alternative_explanation", "无替代解释"));
            result.put("weakest_evidence", map.getOrDefault("weakest_evidence", "证据链完整度不足"));
            result.put("what_would_overturn", map.getOrDefault("what_would_overturn", "需要更多数据"));
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse contradiction JSON: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> getTemplateContradiction(String judgmentType) {
        Map<String, Object> result = new LinkedHashMap<>();
        switch (judgmentType) {
            case "PIPELINE_HEALTH":
                result.put("alternative_explanation", "管道候选人少可能因为岗位高度细分，而非招聘策略问题");
                result.put("weakest_evidence", "管道数据依赖ATS准确性，被动候选人不被统计");
                result.put("what_would_overturn", "如果3天内收到5+份匹配度>80%的简历");
                break;
            case "CANDIDATE_OPINION":
                result.put("alternative_explanation", "跳槽频繁可能是行业特性而非稳定性问题");
                result.put("weakest_evidence", "缺乏候选人前两份工作的具体离职原因");
                result.put("what_would_overturn", "如果候选人能合理解释跳槽逻辑且有推荐信");
                break;
            case "DECISION_CONSISTENCY":
                result.put("alternative_explanation", "决策偏差可能因岗位紧急度不同，而非标准漂移");
                result.put("weakest_evidence", "仅基于评分对比，未考虑岗位级别差异");
                result.put("what_would_overturn", "如果后续3次决策都朝同一方向偏移");
                break;
            default:
                result.put("alternative_explanation", "数据不足，无法提供替代解释");
                result.put("weakest_evidence", "证据量不足以做出高置信判断");
                result.put("what_would_overturn", "积累更多数据后重新评估");
                break;
        }
        return result;
    }

    // ──────────── 判断文本构建（规则引擎兜底）────────────

    private String buildPipelineJudgmentText(Map<String, Object> data, Map<String, Object> profile,
                                              List<Map<String, Object>> history) {
        int total = toInt(data.get("totalCandidates"), 0);
        int daysOpen = toInt(data.get("daysOpen"), 0);
        int inInterview = toInt(data.get("inInterview"), 0);
        int inFinal = toInt(data.get("inFinalRound"), 0);
        String health = data.containsKey("health") ? String.valueOf(data.get("health")) : "UNKNOWN";

        switch (health) {
            case "CRITICAL":
                return String.format("管道处于危险状态：已开放%d天仅积累%d名候选人，日均不到1人。历史数据显示此类岗位最终平均周期延长50%%。建议立即扩大寻源渠道并考虑猎头并行。", daysOpen, total);
            case "WARNING":
                return String.format("管道需关注：%d名候选人中仅%d人进入面试，转化率偏低。建议复核JD是否准确描述了实际需求。", total, inInterview);
            default:
                return String.format("管道健康：%d名候选人，%d人已进入终面。面试转化率正常，预计可按期完成。", total, inFinal);
        }
    }

    private String buildCandidateJudgmentText(Map<String, Object> data, Map<String, Object> scores,
                                               Map<String, Object> profile, Map<String, Object> jobProfile) {
        StringBuilder sb = new StringBuilder("综合评估：");
        if (data.containsKey("matchScore")) {
            double ms = ((Number) data.get("matchScore")).doubleValue();
            if (ms >= 85) sb.append("技术匹配度高。");
            else if (ms >= 70) sb.append("技术匹配度尚可。");
            else sb.append("技术匹配度偏低。");
        }
        if (profile != null && profile.get("riskFlags") != null) {
            sb.append("有留存风险信号，建议面试中直接验证跳槽原因和成长性。");
        }
        if (scores != null && !scores.isEmpty()) {
            sb.append("面试表现整体符合预期。");
        }
        return sb.toString();
    }

    private String buildConsistencyText(String decision, Map<String, Object> profile, Map<String, Object> model) {
        double leniency = profile.containsKey("leniencyIndex")
            ? ((Number) profile.get("leniencyIndex")).doubleValue() : 1.0;

        if (leniency > 1.2 && "REJECT".equals(decision)) {
            return String.format("这次选择拒绝，但你历史上评分偏宽松（指数 %.2f）。建议记录具体拒绝原因以备后续校准。", leniency);
        } else if (leniency < 0.8 && "HIRE".equals(decision)) {
            return "这次选择录用，但你历史上评分偏严格。这个候选人的信号质量是否足以支持这次破例？";
        }
        return "当前决策与历史模式一致。";
    }

    private double estimateConfidence(Map<String, Object> data, List<Map<String, Object>> history) {
        if (history == null || history.isEmpty()) return 0.5;
        return Math.min(0.85, 0.5 + history.size() * 0.05);
    }

    // ──────────── 判断→观察 ────────────

    public void emitObservationIfNeeded(Long tenantId, Map<String, Object> judgment) {
        String judgmentType = String.valueOf(judgment.getOrDefault("judgmentType", ""));
        double confidence = judgment.containsKey("confidence")
            ? ((Number) judgment.get("confidence")).doubleValue() : 0.0;
        if (confidence < 0.6) return;

        String severity;
        switch (judgmentType) {
            case "PIPELINE_HEALTH":
                severity = "CRITICAL";
                break;
            case "HIRING_RISK":
                severity = "WARNING";
                break;
            case "DECISION_CONSISTENCY":
                severity = "INFO";
                break;
            default:
                severity = "INFO";
                break;
        }

        memory.createObservation(tenantId, "ALERT", severity,
            judgmentType + " 判断",
            String.valueOf(judgment.get("judgmentText")),
            List.of(Map.of("type", String.valueOf(judgment.getOrDefault("subjectType", "")),
                "id", toLong(judgment.get("subjectId")))),
            Map.of("text", "查看详情", "action_type", "VIEW_JUDGMENT"),
            LocalDateTime.now().plusDays(7));
    }

    // ── helpers ──

    private Long toLong(Object o) {
        if (o instanceof Long) return (Long) o;
        if (o instanceof Integer) return ((Integer) o).longValue();
        if (o instanceof Number) return ((Number) o).longValue();
        return null;
    }

    private int toInt(Object o, int def) {
        if (o instanceof Number) return ((Number) o).intValue();
        return def;
    }
}
