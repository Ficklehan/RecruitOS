package com.recruitos.brain.controller;

import com.recruitos.brain.engine.CoPilotService;
import com.recruitos.common.result.R;
import com.recruitos.common.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * CoPilotController — AI Co-Pilot REST API。
 *
 * 提供对话式 AI 助手的全部接口：
 * - /chat        多轮对话
 * - /diagnose    需求诊断
 * - /evaluate    候选人评估
 * - /search      人才搜索策略
 * - /history     会话历史
 * - /clear       清空会话
 */
@RestController
@RequestMapping("/api/copilot")
public class CoPilotController {
    private static final Logger log = LoggerFactory.getLogger(CoPilotController.class);

    @Resource private CoPilotService coPilotService;

    /**
     * 多轮对话接口。
     *
     * Request body:
     * {
     *   "sessionId": "optional-session-id",
     *   "message": "用户消息",
     *   "contextPage": "当前页面标识（可选，如 /job/list）"
     * }
     */
    @PostMapping("/chat")
    public R<Map<String, Object>> chat(@RequestBody Map<String, Object> body) {
        String sessionId = (String) body.getOrDefault("sessionId", UUID.randomUUID().toString());
        String message = (String) body.getOrDefault("message", "");
        String contextPage = (String) body.getOrDefault("contextPage", "");

        if (message.isEmpty()) {
            return R.fail("消息不能为空");
        }

        log.info("CoPilot chat | tenant={} | session={} | page={}",
            TenantContext.getTenantId(), sessionId.substring(0, Math.min(8, sessionId.length())), contextPage);

        Map<String, Object> result = coPilotService.chat(sessionId, message, contextPage);
        return R.ok(result);
    }

    /**
     * 需求诊断接口 — 结构化分析业务目标与团队缺口。
     *
     * Request body:
     * {
     *   "sessionId": "optional",
     *   "businessObjective": "业务目标描述",
     *   "departmentId": 123 (optional)
     * }
     */
    @PostMapping("/diagnose")
    public R<Map<String, Object>> diagnoseDemand(@RequestBody Map<String, Object> body) {
        String sessionId = (String) body.getOrDefault("sessionId", UUID.randomUUID().toString());
        String objective = (String) body.getOrDefault("businessObjective", "");
        Long deptId = toLong(body.get("departmentId"));

        if (objective.isEmpty()) {
            return R.fail("业务目标不能为空");
        }

        log.info("CoPilot diagnose | tenant={} | objective={}",
            TenantContext.getTenantId(), objective.substring(0, Math.min(50, objective.length())));

        Map<String, Object> result = coPilotService.diagnoseDemand(sessionId, objective, deptId);
        return R.ok(result);
    }

    /**
     * 候选人深度评估接口。
     *
     * Request body:
     * {
     *   "sessionId": "optional",
     *   "candidateInfo": "候选人信息（简历摘要/评估数据）",
     *   "jobContext": "岗位背景"
     * }
     */
    @PostMapping("/evaluate")
    public R<Map<String, Object>> evaluateCandidate(@RequestBody Map<String, Object> body) {
        String sessionId = (String) body.getOrDefault("sessionId", UUID.randomUUID().toString());
        String candidateInfo = (String) body.getOrDefault("candidateInfo", "");
        String jobContext = (String) body.getOrDefault("jobContext", "");

        if (candidateInfo.isEmpty()) {
            return R.fail("候选人信息不能为空");
        }

        log.info("CoPilot evaluate | tenant={}", TenantContext.getTenantId());

        Map<String, Object> result = coPilotService.evaluateCandidate(sessionId, candidateInfo, jobContext);
        return R.ok(result);
    }

    /**
     * 人才搜寻策略接口。
     *
     * Request body:
     * {
     *   "sessionId": "optional",
     *   "jobDescription": "岗位描述",
     *   "constraints": "约束条件（如预算、地点、行业偏好等）"
     * }
     */
    @PostMapping("/search-strategy")
    public R<Map<String, Object>> searchStrategy(@RequestBody Map<String, Object> body) {
        String sessionId = (String) body.getOrDefault("sessionId", UUID.randomUUID().toString());
        String jobDesc = (String) body.getOrDefault("jobDescription", "");
        String constraints = (String) body.getOrDefault("constraints", "");

        if (jobDesc.isEmpty()) {
            return R.fail("岗位描述不能为空");
        }

        log.info("CoPilot search | tenant={}", TenantContext.getTenantId());

        Map<String, Object> result = coPilotService.searchStrategy(sessionId, jobDesc, constraints);
        return R.ok(result);
    }

    /**
     * 获取会话历史。
     */
    @GetMapping("/history/{sessionId}")
    public R<List<Map<String, String>>> getHistory(@PathVariable String sessionId) {
        return R.ok(coPilotService.getSessionHistory(sessionId));
    }

    /**
     * 清空会话（开始新对话）。
     */
    @DeleteMapping("/session/{sessionId}")
    public R<Map<String, String>> clearSession(@PathVariable String sessionId) {
        coPilotService.clearSession(sessionId);
        return R.ok(Map.of("status", "cleared", "sessionId", sessionId));
    }

    /**
     * 获取 Quick Actions 建议 — 根据当前页面推荐 AI 能力。
     */
    @GetMapping("/quick-actions")
    public R<List<Map<String, Object>>> getQuickActions(
            @RequestParam(defaultValue = "") String contextPage) {
        List<Map<String, Object>> actions = buildQuickActions(contextPage);
        return R.ok(actions);
    }

    // ===== helpers =====

    private Long toLong(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        return null;
    }

    private List<Map<String, Object>> buildQuickActions(String contextPage) {
        List<Map<String, Object>> actions = new ArrayList<>();

        // 通用 actions
        if (contextPage.isEmpty() || contextPage.contains("dashboard")) {
            actions.add(qa("🔍 诊断团队缺口", "帮我分析当前团队的招聘优先级", "diagnose", "demand"));
            actions.add(qa("📊 查看招聘健康度", "分析我的招聘流程哪里可以优化", "analyze", "health"));
        }

        if (contextPage.contains("demand") || contextPage.contains("job")) {
            actions.add(qa("🎯 AI 需求诊断", "我有一个业务目标，帮我分析该招什么人", "diagnose", "demand"));
            actions.add(qa("🔎 人才搜索策略", "帮我制定这个岗位的找人策略", "search", "search"));
            actions.add(qa("📝 JD 优化建议", "帮我优化这个岗位的 JD", "optimize", "jd"));
        }

        if (contextPage.contains("candidate")) {
            actions.add(qa("👤 深度评估候选人", "用 Bar Raiser 标准评估这个候选人", "evaluate", "candidate"));
            actions.add(qa("💭 意向预测解读", "这个候选人真的会来吗？", "predict", "intent"));
        }

        if (contextPage.contains("interview")) {
            actions.add(qa("🎤 面试问题建议", "为这个候选人设计结构化面试问题", "design", "interview"));
            actions.add(qa("⚖️ 偏差检测", "检查我的面试评分有没有潜在偏差", "check", "bias"));
            actions.add(qa("🔄 校准会准备", "帮我准备多面试官校准会材料", "prepare", "calibration"));
        }

        if (contextPage.contains("offer")) {
            actions.add(qa("💰 Offer 策略分析", "这个候选人该出多少钱？怎么谈？", "strategize", "offer"));
            actions.add(qa("📋 谈判话术", "帮我生成 Offer 谈判的沟通要点", "generate", "script"));
        }

        return actions;
    }

    private Map<String, Object> qa(String label, String prompt, String action, String touchpoint) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("prompt", prompt);
        m.put("action", action);
        m.put("touchpoint", touchpoint);
        return m;
    }
}
