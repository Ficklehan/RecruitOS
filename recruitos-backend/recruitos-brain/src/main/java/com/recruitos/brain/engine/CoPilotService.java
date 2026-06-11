package com.recruitos.brain.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitos.brain.aggregator.BrainDataAggregator;
import com.recruitos.common.llm.LlmChatRequest;
import com.recruitos.common.llm.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CoPilotService — 世界顶尖 AI 产品经理 + 招聘专家 智能体。
 *
 * 这个服务不是简单的 LLM wrapper。它编码了来自 Google、Meta、Amazon、Stripe、
 * Netflix、Airbnb、字节跳动等世界顶级公司的 PM 方法论和招聘实践。
 *
 * 核心能力：
 * 1. 业务需求诊断 — 从业务目标倒推人才需求，而不是从 JD 出发
 * 2. 候选人深度评估 — 多维信号融合，超越简历表面
 * 3. 人才搜索策略 — 知道在哪里找到对的人，怎么吸引他们
 * 4. 战略招聘咨询 — 招聘不是填坑，是构建组织能力
 * 5. 面试与评估设计 — 结构化、可预测、防偏差
 */
@Service
public class CoPilotService {
    private static final Logger log = LoggerFactory.getLogger(CoPilotService.class);

    @Resource private LlmClient llmClient;
    @Resource private ObjectMapper objectMapper;
    @Resource private BrainDataAggregator aggregator;

    // 会话上下文缓存 (sessionId -> conversation history)
    private final Map<String, List<Map<String, String>>> sessions = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY = 20;

    // ========================================================================
    // 系统提示词 — 世界顶级 PM + 招聘专家的思维模型
    // ========================================================================

    private static final String COPILOT_SYSTEM_PROMPT =
        """
        你是 RecruitOS AI Co-Pilot，一个世界顶尖的 AI 产品经理兼招聘战略顾问。你的思维融合了 Google、
        Meta、Amazon、Stripe、Netflix、Airbnb 和字节跳动等全球顶级公司的最佳实践。

        ## 你的双重身份

        作为**产品经理**，你：
        - 从业务目标出发思考，而非从岗位描述出发
        - 理解「招什么样的人」源于「要解决什么问题」
        - 知道一个岗位的背后是能力组合，不是职称
        - 擅长评估投入产出比：招人的成本 vs 不招人的代价
        - 能用 Amazon 的 Working Backwards 思维：从客户需求倒推组织能力

        作为**招聘专家**，你：
        - 深谙 Google 的结构化面试方法论：定义维度 → 设计问题 → 校准评分
        - 懂得 Meta 的「Bar Raiser」制度：独立第三方确保标准不降
        - 掌握 Stripe 的「Writing Culture」招人哲学：写清楚比说清楚更重要
        - 理解 Netflix 的「Talent Density」理念：一个顶尖人才 > 三个平庸的人
        - 熟悉字节跳动的「ROI 驱动招聘」：每个岗位都有明确的业务回报预期

        ## 你的核心思维框架

        ### 需求诊断五步法
        1. **业务目标解码**：这个需求背后是什么业务问题？是真需求还是焦虑？
        2. **现有能力审计**：团队现在有什么能力？缺口在哪里？
        3. **替代方案穷举**：不招人能否解决？外包？工具化？内部转岗？
        4. **ROI 量化**：这个人带来的增量价值 vs 招聘+薪酬总成本
        5. **画像定义**：如果确认要招，什么样的人是最优解？

        ### 候选人评估六维模型
        1. **核心能力** (权重 35%)：硬技能与岗位的匹配度
        2. **影响力** (权重 20%)：能否推动跨团队协作、向上管理
        3. **成长性** (权重 15%)：学习速度、适应变化的能力
        4. **文化贡献** (权重 15%)：带来什么新视角，而非仅仅「文化契合」
        5. **动机匹配** (权重 10%)：为什么想加入，与团队阶段是否匹配
        6. **风险因子** (权重 5%)：跳槽频率、职业空窗、行业转换

        ### 人才搜寻四象限
        - **主动候选人**：在看机会的 → 渠道运营、内推
        - **被动候选人**：不主动看的 → 内容吸引、口碑传播
        - **潜在候选人**：未来可能适合的 → 人才池培育、社区运营
        - **不可能候选人**：稀缺顶尖人才 → 定向猎寻、使命驱动

        ## 你的对话风格

        - **直接但有温度**：像一位经验丰富的 VP of Product 在跟你聊，不装、不绕
        - **先问后答**：不确定上下文时，先问关键澄清问题
        - **结构化输出**：复杂分析用清晰的分层结构呈现
        - **永远有行动建议**：每个分析至少带一个可执行下一步
        - **诚实标注置信度**：有数据支撑的判断 vs 基于经验的推测，说清楚
        - **引用实践来源**：当你给出建议时，标注灵感来自哪家公司的实践

        ## 关键原则

        1. **Insight → Action**：每条洞察必须通向一个可执行的操作
        2. **人在回路**：你做分析和建议，让人类做价值判断
        3. **反脆弱招聘**：招人不是为了填坑，是为了让组织变得更强
        4. **速度与质量平衡**：知道什么时候要快（常规岗），什么时候要慢（关键岗）
        5. **数据驱动但非数据奴役**：数据告诉你 what，经验告诉你 why

        现在，作为 RecruitOS AI Co-Pilot，开始你的工作。
        """;

    /**
     * 需求诊断专用系统提示词 — 深度业务分析模式
     */
    private static final String DEMAND_DIAGNOSIS_SYSTEM =
        """
        你现在以「AI 产品经理 + 招聘战略顾问」的身份进行需求诊断。

        请按以下结构分析用户输入的业务需求：

        ## 第一步：业务解码
        - 这个需求的业务背景是什么？
        - 关键成功指标是什么？（量化）
        - 时间紧迫度如何？

        ## 第二步：能力审计
        - 当前团队能力画像
        - 与业务目标的能力缺口
        - 缺口严重度评估

        ## 第三步：方案比选
        - 方案A：招人（什么画像？什么级别？）
        - 方案B：内部调配/培养
        - 方案C：外包/工具替代
        - 推荐方案及理由

        ## 第四步：如果招人
        - 岗位画像（必备技能、加分技能、软素质）
        - 建议渠道组合
        - 面试维度设计（4-6个维度，含权重）
        - 面试官建议
        - 预期招聘周期和预算范围

        ## 第五步：风险提示
        - 这个需求可能的陷阱
        - 招错人的代价预估
        - 降低风险的策略

        输出格式：清晰的结构化 Markdown，每个部分都有实质内容，不是空洞模板。
        在建议中引用具体公司的实践（标注公司名）。
        """;

    /**
     * 候选人评估专用系统提示词 — Bar Raiser 视角
     */
    private static final String CANDIDATE_EVALUATION_SYSTEM =
        """
        你现在以「Bar Raiser + 资深招聘专家」的身份评估候选人。

        使用六维评估模型：

        1. **核心能力** (35%)：硬技能与岗位需求的实际匹配度
        2. **影响力** (20%)：过往项目的 scope、跨团队推动力、向上管理能力
        3. **成长性** (15%)：职业生涯轨迹、学习速度、新领域适应力
        4. **文化贡献** (15%)：能带来什么新视角和能力互补，而非仅仅「合得来」
        5. **动机匹配** (10%)：为什么是现在？为什么是我们？
        6. **风险因子** (5%)：跳槽模式、稳定性信号

        每个维度：
        - 给出 1-5 分的评分
        - 引用具体证据（简历中的哪句话/哪个经历）
        - 标注信号强度（强信号/中信号/弱信号/推断）

        最后输出：
        - 综合评估（Strong Hire / Hire / Lean Hire / Lean No Hire / No Hire / Strong No Hire）
        - 关键风险点
        - 如果推进，面试中需要验证的 3 个关键假设
        - 面试维度建议（权重分配 + 每个维度的考察重点）

        评判标准：对标行业前 25% 的候选人，不是「还不错」的及格线。
        引用 Amazon Bar Raiser 的核心理念：每次招聘都应该提升团队的平均水平。
        """;

    // ========================================================================
    // 公共 API
    // ========================================================================

    /**
     * 发起或继续对话。
     */
    public Map<String, Object> chat(String sessionId, String userMessage, String contextPage) {
        // 获取或创建会话历史
        List<Map<String, String>> history = sessions.computeIfAbsent(sessionId, k -> new ArrayList<>());

        // 构建增强的用户消息（带上页面上下文）
        String enhancedMessage = buildEnhancedMessage(userMessage, contextPage);

        // 保存用户消息到历史
        Map<String, String> userTurn = new LinkedHashMap<>();
        userTurn.put("role", "user");
        userTurn.put("content", userMessage);
        history.add(userTurn);

        // 裁剪历史到最近 N 轮
        while (history.size() > MAX_HISTORY) {
            history.remove(0);
        }

        // 构建 LLM 请求
        String conversationContext = buildConversationContext(history);
        String fullUserPrompt = enhancedMessage;
        if (!conversationContext.isEmpty()) {
            fullUserPrompt = "对话历史：\n" + conversationContext + "\n---\n当前问题：" + enhancedMessage;
        }

        LlmChatRequest req = new LlmChatRequest();
        req.setSystemPrompt(COPILOT_SYSTEM_PROMPT);
        req.setUserPrompt(fullUserPrompt);
        req.setScenario("copilot_chat");

        String aiResponse = llmClient.chat(req);

        // 如果 LLM 不可用，给出降级响应
        if (aiResponse == null || aiResponse.isEmpty()) {
            aiResponse = generateFallbackResponse(userMessage, contextPage);
        }

        // 解析 AI 响应中的 actions 和结构化数据
        Map<String, Object> parsed = parseResponse(aiResponse);

        // 保存 AI 回复到历史
        Map<String, String> aiTurn = new LinkedHashMap<>();
        aiTurn.put("role", "assistant");
        aiTurn.put("content", aiResponse);
        history.add(aiTurn);

        // 构建返回
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("message", aiResponse);
        result.put("suggestedActions", parsed.getOrDefault("actions", Collections.emptyList()));
        result.put("confidence", parsed.getOrDefault("confidence", 0.75));
        result.put("touchpointHint", parsed.getOrDefault("touchpointHint", null));

        return result;
    }

    /**
     * 需求诊断 — 结构化输出团队缺口分析。
     */
    public Map<String, Object> diagnoseDemand(String sessionId, String businessObjective, Long departmentId) {
        List<Map<String, String>> history = sessions.computeIfAbsent(sessionId, k -> new ArrayList<>());

        String contextPrompt = buildDemandContext(businessObjective, departmentId);

        LlmChatRequest req = new LlmChatRequest();
        req.setSystemPrompt(DEMAND_DIAGNOSIS_SYSTEM);
        req.setUserPrompt(contextPrompt);
        req.setScenario("demand_diagnosis");

        String response = llmClient.chat(req);

        if (response == null || response.isEmpty()) {
            response = "## 业务需求诊断\n\n### 业务解码\n基于您描述的「" + truncate(businessObjective, 80) + "」，这是一个值得深入分析的需求。\n\n### 能力缺口\n由于当前缺少团队数据，我暂时无法给出量化的能力缺口分析。建议您先在系统中完善团队成员的技能标签。\n\n### 初步建议\n在没有更多上下文的情况下，我建议先做以下几步：\n1. 明确业务的关键成功指标（KPI/OKR）\n2. 梳理现有团队成员的能力矩阵\n3. 评估是否有内部调配的可能性\n\n请在系统中补充更多信息后，我可以给出更精准的诊断。";
        }

        Map<String, String> aiTurn = new LinkedHashMap<>();
        aiTurn.put("role", "assistant");
        aiTurn.put("content", response);
        history.add(aiTurn);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("diagnosis", response);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * 候选人深度评估。
     */
    public Map<String, Object> evaluateCandidate(String sessionId, String candidateInfo, String jobContext) {
        List<Map<String, String>> history = sessions.computeIfAbsent(sessionId, k -> new ArrayList<>());

        String evalPrompt = "请评估以下候选人：\n\n**候选人信息**\n" + candidateInfo + "\n\n**岗位背景**\n" + jobContext;

        LlmChatRequest req = new LlmChatRequest();
        req.setSystemPrompt(CANDIDATE_EVALUATION_SYSTEM);
        req.setUserPrompt(evalPrompt);
        req.setScenario("candidate_evaluation");

        String response = llmClient.chat(req);

        if (response == null || response.isEmpty()) {
            response = "## 候选人评估\n\n当前无法获取 AI 评估结果。请在系统中完善候选人数据和岗位信息后重试。";
        }

        Map<String, String> aiTurn = new LinkedHashMap<>();
        aiTurn.put("role", "assistant");
        aiTurn.put("content", response);
        history.add(aiTurn);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("evaluation", response);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * 人才搜寻策略建议。
     */
    public Map<String, Object> searchStrategy(String sessionId, String jobDescription, String constraints) {
        String searchPrompt = "我需要为以下岗位制定人才搜寻策略：\n\n**岗位描述**\n" + jobDescription + "\n\n**约束条件**\n" + (constraints.isEmpty() ? "无特别约束" : constraints);

        LlmChatRequest req = new LlmChatRequest();
        req.setSystemPrompt(COPILOT_SYSTEM_PROMPT + "\n\n请聚焦于人才搜寻策略。使用四象限模型分析，给出具体的渠道、关键词、吸引策略和时间线。");
        req.setUserPrompt(searchPrompt);
        req.setScenario("search_strategy");

        String response = llmClient.chat(req);

        if (response == null || response.isEmpty()) {
            response = "## 人才搜寻策略\n\n### 渠道建议\n- **主动渠道**：Boss直聘、脉脉、LinkedIn — 针对明确在求职的候选人\n- **被动渠道**：GitHub、技术博客、行业会议 — 通过内容吸引\n- **猎头渠道**：关键岗位建议使用垂直领域猎头\n\n### 搜索关键词\n需要更具体的岗位信息来生成精准搜索关键词。";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("strategy", response);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * 获取会话历史。
     */
    public List<Map<String, String>> getSessionHistory(String sessionId) {
        return sessions.getOrDefault(sessionId, Collections.emptyList());
    }

    /**
     * 清空会话。
     */
    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
    }

    // ========================================================================
    // 私有方法
    // ========================================================================

    private String buildEnhancedMessage(String userMessage, String contextPage) {
        if (contextPage == null || contextPage.isEmpty()) {
            return userMessage;
        }
        return "[当前页面：" + contextPage + "]\n" + userMessage;
    }

    private String buildConversationContext(List<Map<String, String>> history) {
        if (history.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        // 只取最近 6 轮用作上下文，避免太长
        int start = Math.max(0, history.size() - 6);
        for (int i = start; i < history.size(); i++) {
            Map<String, String> turn = history.get(i);
            String role = "user".equals(turn.get("role")) ? "用户" : "AI";
            sb.append(role).append("：").append(turn.get("content")).append("\n");
        }
        return sb.toString();
    }

    private String buildDemandContext(String objective, Long deptId) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 业务需求诊断请求\n\n");
        sb.append("**业务目标**：").append(objective).append("\n\n");

        if (deptId != null) {
            try {
                Map<String, Object> teamData = aggregator.fetchTeamData(deptId);
                sb.append("**团队数据**：\n```json\n");
                sb.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(teamData));
                sb.append("\n```\n");
            } catch (Exception e) {
                sb.append("**团队数据**：暂无\n");
            }
        }

        sb.append("\n请按照需求诊断五步法进行完整分析。");
        return sb.toString();
    }

    /**
     * 解析 AI 响应中的结构化数据（尝试提取 JSON actions）
     */
    private Map<String, Object> parseResponse(String response) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("confidence", 0.75);
        result.put("actions", Collections.emptyList());
        try {
            // 尝试提取 JSON 块
            int jsonStart = response.indexOf("```json");
            if (jsonStart >= 0) {
                int contentStart = response.indexOf("\n", jsonStart) + 1;
                int jsonEnd = response.indexOf("```", contentStart);
                if (jsonEnd >= 0) {
                    String jsonStr = response.substring(contentStart, jsonEnd).trim();
                    Map<String, Object> parsed = objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
                    if (parsed.containsKey("confidence")) result.put("confidence", parsed.get("confidence"));
                    if (parsed.containsKey("actions")) result.put("actions", parsed.get("actions"));
                    if (parsed.containsKey("touchpoint")) result.put("touchpointHint", parsed.get("touchpoint"));
                }
            }
        } catch (Exception ignored) {
            // 不是所有回复都包含 JSON，正常情况
        }
        return result;
    }

    /**
     * 当 LLM 不可用时的智能降级响应。
     * 这不是简单的兜底话术，而是基于规则引擎的实质建议。
     */
    private String generateFallbackResponse(String userMessage, String contextPage) {
        String msg = userMessage.toLowerCase();

        // 需求诊断相关
        if (containsAny(msg, "招人", "招聘需求", "招什么", "岗位", "缺人", "hire", "headcount")) {
            return """
                ## 需求诊断（离线模式）

                在没有 AI 实时推理的情况下，让我用一个结构化的框架帮你思考：

                ### 🔍 先问自己三个问题（Google 的「Pre-mortem」方法）
                1. **如果不招这个人，6个月后会发生什么？**（最坏情况）
                2. **这个人的工作产出，能不能被现有团队消化/工具替代？**（替代方案）
                3. **招错这个人的代价是什么？**（风险量化）

                ### 📋 建议填写的需求卡片
                - 业务目标（一句话）
                - 关键结果（可量化指标）
                - 现有团队能力缺口
                - 时间紧迫度（立即 / 本季度 / 半年内）

                ### 💡 通用参考
                - **关键岗位**（影响业务命脉）：建议使用 Bar Raiser 流程，多位面试官独立评估
                - **常规岗位**：可以适当加快流程，但不要跳过结构化面试维度
                - **创业期团队**：优先招「T型人才」— 有一项极深 + 多项够用
                - **成熟期团队**：优先招「专精人才」— 在特定领域能立刻贡献

                > 这些建议基于 Google、Meta、Amazon 的招聘实践总结。

                补充更多业务上下文后，我可以给出更精准的建议。
                """;
        }

        // 候选人评估相关
        if (containsAny(msg, "候选人", "评估", "简历", "面试", "candidate", "evaluate", "resume")) {
            return """
                ## 候选人评估框架（离线模式）

                在没有 AI 实时分析的情况下，请使用以下六维框架手动评估：

                ### 六维评估表
                | 维度 | 权重 | 评分(1-5) | 关键证据 |
                |------|------|-----------|----------|
                | 核心能力 | 35% | ? | 简历中哪段经历最能证明 |
                | 影响力 | 20% | ? | 项目 scope、跨团队协作 |
                | 成长性 | 15% | ? | 职业轨迹斜率 |
                | 文化贡献 | 15% | ? | 能带来什么新的视角 |
                | 动机匹配 | 10% | ? | 为什么是现在/我们 |
                | 风险因子 | 5% | ? | 跳槽模式、稳定性 |

                ### 👁️ 重点关注信号
                - **强信号**：量化成果、开源贡献、晋升速度
                - **中信号**：项目描述、团队规模、技术栈
                - **弱信号**：自我评价、证书、课程
                - **红旗**：频繁跳槽（<1年）、职责递减、回避具体细节

                ### 📌 面试建议
                如果推进面试，请确保：
                1. 每个维度至少 2 个问题
                2. 使用 STAR 方法追问（Situation-Task-Action-Result）
                3. 面试后立即记录评分（不超过 30 分钟）
                4. 不同面试官覆盖不同维度，避免重复

                > 评估框架参考 Amazon Bar Raiser + Google 结构化面试实践。
                """;
        }

        // 通用回复
        return """
            ## RecruitOS AI Co-Pilot（离线模式）

            当前 AI 推理服务暂时不可用。以下是你可以做的：

            ### 📌 我可以帮助的领域
            - **需求诊断**：帮你分析「该不该招人、招什么人」
            - **候选人评估**：多维评分框架，Bar Raiser 视角
            - **搜索策略**：去哪里找人、怎么吸引人
            - **面试设计**：结构化面试维度、问题库设计
            - **Offer 策略**：薪酬建议、谈判框架
            - **入职跟踪**：30/60/90天 check-in 设计

            ### 💬 试试这样问我
            - 「支付团队 Q4 要扛 10 倍交易量，现有 4 人，都没做过大规模分布式系统，我该怎么办？」
            - 「拿到一份 P8 后端候选人的简历，帮我用 Bar Raiser 标准评估一下」
            - 「AI 算法专家岗，在 Boss 直聘上搜了一个月没找到合适的人，帮我重新规划搜索策略」

            请描述你的具体场景，我会尽我所能给出结构化的建议。
            """;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
