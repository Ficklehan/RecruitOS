package com.recruitos.brain.controller;

import com.recruitos.brain.service.CognitiveMemoryService;
import com.recruitos.brain.engine.CognitiveJudgmentEngine;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * 认知层 API
 * 暴露记忆、判断、观察的查询和操作接口
 */
@RestController
@RequestMapping("/api/cognitive")
public class CognitiveController {

    @Resource
    private CognitiveMemoryService memory;

    @Resource
    private CognitiveJudgmentEngine judgmentEngine;

    private Map<String, Object> ok(Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("msg", "success");
        result.put("data", data);
        return result;
    }

    // ================================================================
    // 观察（AI 主动推送）— 菜单 AI 状态的数据源
    // ================================================================

    /**
     * 获取待处理观察列表（按严重程度分级）
     */
    @GetMapping("/observations")
    public Map<String, Object> getObservations(
            @RequestHeader("X-Tenant-Id") Long tenantId) {
        List<Map<String, Object>> all = memory.getPendingObservations(tenantId);
        Map<String, Integer> counts = memory.getObservationCounts(tenantId);

        // 按严重程度分组
        List<Map<String, Object>> critical = new ArrayList<>();
        List<Map<String, Object>> warnings = new ArrayList<>();
        List<Map<String, Object>> infos = new ArrayList<>();

        for (Map<String, Object> obs : all) {
            String severity = (String) obs.get("severity");
            switch (severity) {
                case "CRITICAL":
                    critical.add(obs);
                    break;
                case "WARNING":
                    warnings.add(obs);
                    break;
                default:
                    infos.add(obs);
                    break;
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("critical", critical);
        data.put("warnings", warnings);
        data.put("infos", infos);
        data.put("counts", counts);
        return ok(data);
    }

    /**
     * 获取菜单 AI 状态徽标数据（精简版，供前端菜单轮询）
     */
    @GetMapping("/menu-status")
    public Map<String, Object> getMenuStatus(
            @RequestHeader("X-Tenant-Id") Long tenantId) {
        Map<String, Integer> counts = memory.getObservationCounts(tenantId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("insightAlerts", counts.get("critical"));
        data.put("insightAttention", counts.get("warning"));
        data.put("insightObserve", counts.get("info"));
        data.put("totalPending", counts.get("total"));
        return ok(data);
    }

    /**
     * 标记观察已处理
     */
    @PostMapping("/observations/{id}/action")
    public Map<String, Object> actionObservation(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String action = (String) body.getOrDefault("action", "EXECUTED");
        Long userId = body.containsKey("userId")
            ? ((Number) body.get("userId")).longValue() : null;
        memory.actionObservation(id, action, userId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", true);
        return ok(data);
    }

    /**
     * 反馈观察是否有用
     */
    @PostMapping("/observations/{id}/feedback")
    public Map<String, Object> feedbackObservation(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String feedback = (String) body.get("feedback");
        memory.feedbackObservation(id, feedback);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", true);
        return ok(data);
    }

    // ================================================================
    // 判断
    // ================================================================

    /**
     * 获取某对象的最新 AI 判断
     */
    @GetMapping("/judgments/{subjectType}/{subjectId}")
    public Map<String, Object> getJudgment(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @PathVariable String subjectType,
            @PathVariable Long subjectId) {
        return ok(memory.getLatestJudgment(tenantId, subjectType, subjectId));
    }

    /**
     * 请求 AI 生成管线健康判断
     */
    @PostMapping("/judge/pipeline/{jobId}")
    public Map<String, Object> judgePipeline(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @PathVariable Long jobId,
            @RequestBody Map<String, Object> pipelineData) {
        Map<String, Object> result = judgmentEngine.judgePipelineHealth(tenantId, jobId, pipelineData);

        // 持久化判断
        Long judgmentId = memory.publishJudgment(
            tenantId, "PIPELINE_HEALTH", "JOB", jobId,
            (String) result.get("judgmentText"),
            (Map<String, Object>) result.get("judgmentData"),
            ((Number) result.get("confidence")).doubleValue(),
            (List<Long>) result.get("evidenceIds"),
            (Map<String, Object>) result.get("contradiction")
        );

        // 判断→观察转化
        result.put("judgmentId", judgmentId);
        Map<String, Object> observationData = new LinkedHashMap<>();
        observationData.put("judgmentType", "PIPELINE_HEALTH");
        observationData.put("judgmentText", result.get("judgmentText"));
        observationData.put("confidence", result.get("confidence"));
        observationData.put("subjectType", "JOB");
        observationData.put("subjectId", jobId);
        judgmentEngine.emitObservationIfNeeded(tenantId, observationData);

        return ok(result);
    }

    /**
     * 请求 AI 生成候选人综合判断
     */
    @PostMapping("/judge/candidate")
    public Map<String, Object> judgeCandidate(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestBody Map<String, Object> body) {
        Long candidateId = ((Number) body.get("candidateId")).longValue();
        Long jobId = body.containsKey("jobId")
            ? ((Number) body.get("jobId")).longValue() : null;
        Map<String, Object> candidateData = (Map<String, Object>) body.get("candidateData");
        Map<String, Object> interviewScores = (Map<String, Object>) body.getOrDefault("interviewScores", new LinkedHashMap<>());

        Map<String, Object> result = judgmentEngine.judgeCandidate(
            tenantId, candidateId, jobId, candidateData, interviewScores);

        memory.publishJudgment(
            tenantId, "CANDIDATE_OPINION", "CANDIDATE", candidateId,
            (String) result.get("judgmentText"),
            (Map<String, Object>) result.get("judgmentData"),
            ((Number) result.get("confidence")).doubleValue(),
            (List<Long>) result.get("evidenceIds"),
            (Map<String, Object>) result.get("contradiction")
        );

        return ok(result);
    }

    // ================================================================
    // 用户画像
    // ================================================================

    /**
     * 获取当前用户的认知画像
     */
    @GetMapping("/my-model")
    public Map<String, Object> getMyModel(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId) {
        return ok(memory.getUserModel(tenantId, userId));
    }

    // ================================================================
    // 记忆查询
    // ================================================================

    /**
     * 获取某对象的历史事件时间线
     */
    @GetMapping("/timeline/{subjectType}/{subjectId}")
    public Map<String, Object> getTimeline(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @PathVariable String subjectType,
            @PathVariable Long subjectId) {
        return ok(memory.getObjectEvents(tenantId, subjectType, subjectId));
    }

    /**
     * 获取活跃教训
     */
    @GetMapping("/lessons")
    public Map<String, Object> getLessons(
            @RequestHeader("X-Tenant-Id") Long tenantId) {
        return ok(memory.getActiveLessons(tenantId));
    }

    /**
     * 获取活跃模式
     */
    @GetMapping("/patterns/{patternType}")
    public Map<String, Object> getPatterns(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @PathVariable String patternType) {
        return ok(memory.getPatternsByType(tenantId, patternType));
    }
}
