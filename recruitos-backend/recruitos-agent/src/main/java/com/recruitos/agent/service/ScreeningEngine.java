package com.recruitos.agent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitos.agent.platform.PlatformCandidate;
import com.recruitos.common.match.JdTagMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

@Component
public class ScreeningEngine {

    @Resource
    private ObjectMapper objectMapper;

    public static class ScreeningResult {
        private final boolean passed;
        private final String stage;
        private final String humanReason;
        private final Map<String, Object> skipReasonJson;

        public ScreeningResult(boolean passed, String stage, String humanReason, Map<String, Object> skipReasonJson) {
            this.passed = passed;
            this.stage = stage;
            this.humanReason = humanReason;
            this.skipReasonJson = skipReasonJson;
        }

        public boolean isPassed() { return passed; }
        public String getStage() { return stage; }
        public String getHumanReason() { return humanReason; }
        public Map<String, Object> getSkipReasonJson() { return skipReasonJson; }
    }

    public ScreeningResult evaluateStage1(Object screeningProfileRaw, PlatformCandidate pc,
                                                String jobTagsJson, ObjectMapper objectMapper) {
        Map<String, Object> profile = asMap(screeningProfileRaw);
        double score = scoreFromCandidate(pc);
        double minCard = 45.0;
        JdTagMatcher.JdTagMatchResult jdMatch = buildJdMatch(jobTagsJson, pc, objectMapper);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rules = profile != null
                ? (List<Map<String, Object>>) profile.get("stage1CardRules") : null;

        if (rules == null || rules.isEmpty()) {
            if (score < minCard) {
                return fail("CARD", "卡片筛选：外露信息匹配不足",
                        ruleJson("CARD", "minCardScore", score + " < " + minCard, score, 0.85));
            }
            return pass("CARD");
        }

        for (Map<String, Object> rule : rules) {
            if (!matchRule(rule, pc, score, jdMatch)) {
                String tag = String.valueOf(rule.get("tag"));
                return fail("CARD", "卡片筛选：不满足「" + tag + "」",
                        ruleJson("CARD", tag, "rule not satisfied", score, 0.8));
            }
        }
        return pass("CARD");
    }

    public ScreeningResult evaluateStage2(Object screeningProfileRaw, PlatformCandidate pc,
                                                String jobTagsJson, ObjectMapper objectMapper) {
        Map<String, Object> profile = asMap(screeningProfileRaw);
        double score = scoreFromCandidate(pc);
        double threshold = 60.0;
        if (profile != null && profile.get("passThreshold") instanceof Number) {
            threshold = ((Number) profile.get("passThreshold")).doubleValue();
        }
        JdTagMatcher.JdTagMatchResult jdMatch = buildJdMatch(jobTagsJson, pc, objectMapper);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rules = profile != null
                ? (List<Map<String, Object>>) profile.get("stage2ResumeRules") : null;

        if (rules != null && !rules.isEmpty()) {
            for (Map<String, Object> rule : rules) {
                if (Boolean.TRUE.equals(rule.get("required")) && !matchRule(rule, pc, score, jdMatch)) {
                    String tag = String.valueOf(rule.get("tag"));
                    return fail("FULL_RESUME", "简历筛选：必备项「" + tag + "」未达标",
                            ruleJson("FULL_RESUME", tag, "required rule failed", score, 0.9));
                }
            }
        }

        if (score < threshold) {
            return fail("FULL_RESUME", "简历筛选：综合分 " + (int) score + " 低于及格线 " + (int) threshold + "（基于JdTagMatcher统一计分）",
                    ruleJson("FULL_RESUME", "passThreshold", score + " < " + threshold, score, 0.9));
        }
        return pass("FULL_RESUME");
    }

    /**
     * 统一使用JdTagMatcher进行规则匹配，替代旧版title.contains()。
     * 如果传入了jdMatchResult则使用逐标签匹配结果，否则回退到分数判断。
     */
    private boolean matchRule(Map<String, Object> rule, PlatformCandidate pc, double score,
                              JdTagMatcher.JdTagMatchResult jdMatch) {
        if (Boolean.TRUE.equals(rule.get("required")) && score < 55) {
            return false;
        }
        Object minScore = rule.get("minScore");
        if (minScore instanceof Number && score < ((Number) minScore).doubleValue() * 100) {
            return false;
        }
        String tag = rule.get("tag") != null ? rule.get("tag").toString() : "";
        if (org.springframework.util.StringUtils.hasText(tag)) {
            // 优先使用JdTagMatcher的逐标签匹配结果
            if (jdMatch != null && jdMatch.hasTags()) {
                for (Map<String, Object> row : jdMatch.getJdTagMatches()) {
                    Object name = row.get("name");
                    if (name != null && name.toString().toLowerCase().contains(tag.toLowerCase())) {
                        return Boolean.TRUE.equals(row.get("matched"));
                    }
                }
            }
            // 回退：检查候选人title/company是否包含该标签 + 分数
            if (org.springframework.util.StringUtils.hasText(pc.getTitle())) {
                boolean titleMatch = pc.getTitle().toLowerCase().contains(tag.toLowerCase());
                boolean companyMatch = org.springframework.util.StringUtils.hasText(pc.getCompany())
                    && pc.getCompany().toLowerCase().contains(tag.toLowerCase());
                return (titleMatch || companyMatch) && score >= 50;
            }
            return score >= 60;
        }
        return score >= 50;
    }

    private double scoreFromCandidate(PlatformCandidate pc) {
        if (pc.getMatchScore() != null) {
            return pc.getMatchScore().doubleValue();
        }
        return 50.0;
    }

    private ScreeningResult pass(String stage) {
        return new ScreeningResult(true, stage, null, null);
    }

    private ScreeningResult fail(String stage, String human, Map<String, Object> json) {
        return new ScreeningResult(false, stage, human, json);
    }

    private Map<String, Object> ruleJson(String stage, String rule, String evidence, double score, double confidence) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("stage", stage);
        m.put("rule", rule);
        m.put("evidence", evidence);
        m.put("score", score);
        m.put("confidence", confidence);
        return m;
    }

    /**
     * 构建JdTagMatcher匹配结果，用于统一筛选逻辑。
     */
    private JdTagMatcher.JdTagMatchResult buildJdMatch(String jobTagsJson, PlatformCandidate pc,
                                                        ObjectMapper objectMapper) {
        if (jobTagsJson == null || jobTagsJson.trim().isEmpty()) {
            return new JdTagMatcher.JdTagMatchResult();
        }
        StringBuilder skillBlob = new StringBuilder();
        if (org.springframework.util.StringUtils.hasText(pc.getTitle())) {
            skillBlob.append(pc.getTitle()).append(',');
        }
        if (org.springframework.util.StringUtils.hasText(pc.getCompany())) {
            skillBlob.append(pc.getCompany());
        }
        return JdTagMatcher.match(jobTagsJson, skillBlob.toString(), objectMapper);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Map) {
            return (Map<String, Object>) raw;
        }
        if (raw instanceof String && StringUtils.hasText((String) raw)) {
            try {
                return objectMapper.readValue((String) raw, new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
