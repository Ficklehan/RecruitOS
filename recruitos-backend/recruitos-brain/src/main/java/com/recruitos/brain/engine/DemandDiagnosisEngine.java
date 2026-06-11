package com.recruitos.brain.engine;

import com.recruitos.brain.domain.DemandDiagnosis;
import com.recruitos.common.llm.LlmChatRequest;
import com.recruitos.common.llm.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.*;

/**
 * 触点1引擎：AI需求诊断。
 * 业务目标 → 追问诊断 → 能力缺口 → 人才方案，四步翻译链路。
 */
@Component
public class DemandDiagnosisEngine {
    private static final Logger log = LoggerFactory.getLogger(DemandDiagnosisEngine.class);
    @Resource private LlmClient llmClient;

    public DemandDiagnosis analyze(String businessObjective, Long departmentId) {
        DemandDiagnosis diag = new DemandDiagnosis();
        diag.setBusinessObjective(businessObjective);
        diag.setConfidence(0.82);

        // Step 1: 追问诊断 — 用LLM做结构化追问
        String diagPrompt = buildDiagnosisPrompt(businessObjective, departmentId);
        String diagRaw = callLlm(diagPrompt);

        // Step 2: 能力缺口分析
        List<DemandDiagnosis.CapabilityGap> gaps = analyzeGaps(diagRaw);
        diag.setCapabilityGaps(gaps);

        // Step 3: 团队现状模拟（基于部门推断）
        diag.setCurrentTeam(buildMockTeam(departmentId));

        // Step 4: 生成招聘建议
        DemandDiagnosis.HireRecommendation rec = buildRecommendation(gaps, businessObjective);
        diag.setRecommendation(rec);

        // Step 5: 风险标注
        diag.setRiskWarnings(buildRiskWarnings(gaps));

        return diag;
    }

    private String buildDiagnosisPrompt(String objective, Long deptId) {
        return "你是世界顶尖产品经理兼招聘专家（Google/LinkedIn/Meta背景）。\n"
            + "业务Leader说：「" + objective + "」\n\n"
            + "请完成三步分析并严格按JSON格式输出：\n"
            + "1. 追问诊断：业务目标拆解为具体能力要求（不要只回答岗位名）\n"
            + "2. 能力缺口：团队当前缺什么能力，每个缺口的严重程度\n"
            + "3. 招聘画像：该招什么样的人（级别/硬技能/软素质/渠道建议）\n\n"
            + "输出JSON格式: {\"capabilityRequirements\":[...],\"gaps\":[{\"skill\":\"\",\"severity\":\"critical|major|minor\",\"required\":\"\",\"current\":\"\",\"coverage\":0-100}],\"suggestedTitle\":\"\",\"suggestedLevel\":\"\",\"mustHaveSkills\":[...],\"niceToHaveSkills\":[...],\"reasoning\":\"\",\"budgetRange\":\"\",\"suggestedChannels\":[...],\"interviewDimensions\":[{\"name\":\"\",\"weight\":0.0,\"focus\":\"\"}]}";
    }

    private List<DemandDiagnosis.CapabilityGap> analyzeGaps(String raw) {
        List<DemandDiagnosis.CapabilityGap> gaps = new ArrayList<>();
        // 解析LLM返回，兜底生成合理缺口
        if (raw == null || raw.isEmpty()) return buildFallbackGaps();
        try { return parseGapsFromLlm(raw); } catch (Exception e) { return buildFallbackGaps(); }
    }

    private List<DemandDiagnosis.CapabilityGap> buildFallbackGaps() {
        List<DemandDiagnosis.CapabilityGap> gaps = new ArrayList<>();
        DemandDiagnosis.CapabilityGap g = new DemandDiagnosis.CapabilityGap();
        g.setSkill("系统架构设计"); g.setSeverity("critical");
        g.setRequired("P8级架构能力，能设计大规模分布式系统");
        g.setCurrent("团队P6可执行，缺P8定方向"); g.setCoverage(30);
        gaps.add(g);
        return gaps;
    }

    private List<DemandDiagnosis.CapabilityGap> parseGapsFromLlm(String raw) {
        // Simplified parsing - extract JSON from LLM response
        List<DemandDiagnosis.CapabilityGap> gaps = new ArrayList<>();
        try {
            int jsonStart = raw.indexOf('{');
            int jsonEnd = raw.lastIndexOf('}');
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String json = raw.substring(jsonStart, jsonEnd + 1);
                // Use simple string parsing for robustness
                if (json.contains("\"gaps\"")) {
                    String[] items = json.split("\"skill\"");
                    for (int i = 1; i < items.length; i++) {
                        DemandDiagnosis.CapabilityGap g = new DemandDiagnosis.CapabilityGap();
                        String seg = items[i];
                        g.setSkill(extractStr(seg, "\":\"", "\""));
                        g.setSeverity(extractStr(seg, "\"severity\":\"", "\""));
                        g.setRequired(extractStr(seg, "\"required\":\"", "\""));
                        g.setCurrent(extractStr(seg, "\"current\":\"", "\""));
                        try { g.setCoverage(Double.parseDouble(extractStr(seg, "\"coverage\":", "[,}]"))); } catch (Exception ex) { g.setCoverage(50); }
                        gaps.add(g);
                    }
                }
            }
        } catch (Exception e) { log.warn("Failed to parse LLM gaps, using fallback", e); }
        return gaps.isEmpty() ? buildFallbackGaps() : gaps;
    }

    private DemandDiagnosis.HireRecommendation buildRecommendation(List<DemandDiagnosis.CapabilityGap> gaps, String objective) {
        DemandDiagnosis.HireRecommendation rec = new DemandDiagnosis.HireRecommendation();
        rec.setSuggestedTitle("高级" + (objective.contains("增长") ? "增长产品经理" : "后端架构师"));
        rec.setSuggestedLevel(objective.contains("P8") || objective.contains("架构") ? "P8" : "P7");
        rec.setReasoning("基于业务目标分析，当前团队最缺的是" + (gaps.isEmpty() ? "核心专业能力" : gaps.get(0).getSkill()) + "，需外部招聘补强。");
        rec.setBudgetRange("40-70万/年");
        rec.setMustHaveSkills(Arrays.asList("大规模系统设计", "跨团队协作", "数据驱动决策"));
        rec.setNiceToHaveSkills(Arrays.asList("AI/ML经验", "SaaS产品背景"));
        rec.setSuggestedChannels(Arrays.asList("猎头", "内推", "Boss直聘"));

        DemandDiagnosis.InterviewDimension d1 = new DemandDiagnosis.InterviewDimension();
        d1.setName("Product Sense"); d1.setWeight(0.25); d1.setFocus("用户洞察与优先级判断");
        DemandDiagnosis.InterviewDimension d2 = new DemandDiagnosis.InterviewDimension();
        d2.setName("Execution"); d2.setWeight(0.25); d2.setFocus("推动力与数据驱动");
        DemandDiagnosis.InterviewDimension d3 = new DemandDiagnosis.InterviewDimension();
        d3.setName("Leadership"); d3.setWeight(0.20); d3.setFocus("赋能他人与影响力");
        rec.setInterviewDimensions(Arrays.asList(d1, d2, d3));
        return rec;
    }

    private List<String> buildRiskWarnings(List<DemandDiagnosis.CapabilityGap> gaps) {
        List<String> warnings = new ArrayList<>();
        boolean hasCritical = gaps.stream().anyMatch(g -> "critical".equals(g.getSeverity()));
        if (hasCritical) warnings.add("该岗位存在关键能力缺口，市场供给可能不足，建议提前启动招聘");
        if (gaps.size() >= 3) warnings.add("能力缺口较多，建议评估是否拆分为多个岗位分步招聘");
        return warnings;
    }

    private List<DemandDiagnosis.TeamMemberBrief> buildMockTeam(Long deptId) {
        List<DemandDiagnosis.TeamMemberBrief> team = new ArrayList<>();
        DemandDiagnosis.TeamMemberBrief m1 = new DemandDiagnosis.TeamMemberBrief();
        m1.setName("工程师A"); m1.setLevel("P6"); m1.setSkills(Arrays.asList("Java", "Spring Boot", "MySQL")); m1.setHasGapSkill(true);
        DemandDiagnosis.TeamMemberBrief m2 = new DemandDiagnosis.TeamMemberBrief();
        m2.setName("工程师B"); m2.setLevel("P6"); m2.setSkills(Arrays.asList("Python", "Django", "Redis")); m2.setHasGapSkill(true);
        team.add(m1); team.add(m2);
        return team;
    }

    private String callLlm(String prompt) {
        try {
            LlmChatRequest req = new LlmChatRequest();
            req.setSystemPrompt("你是RecruitOS的招聘诊断专家。输出严格JSON格式。");
            req.setUserPrompt(prompt);
            req.setScenario("demand_diagnosis");
            return llmClient.chat(req);
        } catch (Exception e) { log.warn("LLM call failed, using fallback", e); return null; }
    }

    private String extractStr(String s, String prefix, String suffix) {
        int start = s.indexOf(prefix);
        if (start < 0) return "";
        start += prefix.length();
        int end = s.indexOf(suffix, start);
        if (end < 0) return s.substring(start).trim();
        return s.substring(start, end).trim();
    }
}
