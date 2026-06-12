package com.recruitos.brain.engine;

import com.recruitos.brain.domain.OfferStrategy;
import com.recruitos.brain.service.CognitiveMemoryService;
import com.recruitos.common.llm.LlmChatRequest;
import com.recruitos.common.llm.LlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * Offer 策略引擎 v2 — AI 场景模拟 + 谈判策略。
 * <p>
 * v1 只做了基本薪酬区间计算。v2 增加：
 * - 竞品 Offer 场景模拟（候选人可能有的选择）
 * - 反制策略（竞品给什么我们给什么）
 * - 多方案对比（保守/激进/平衡）
 * - AI 驱动的谈判建议（基于 LLM）
 */
@Component
public class OfferStrategyEngine {

    private static final Logger log = LoggerFactory.getLogger(OfferStrategyEngine.class);

    @Resource
    private CognitiveMemoryService memory;

    @Resource
    private LlmClient llmClient;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 生成 Offer 策略（含场景模拟）。
     */
    public OfferStrategy generate(Long candidateId, String candidateName, Long jobId, String jobTitle,
                                   String jobLevel, Map<String, Object> context) {
        OfferStrategy os = new OfferStrategy();
        os.setCandidateId(candidateId);
        os.setCandidateName(candidateName);
        os.setJobId(jobId);
        os.setJobTitle(jobTitle);
        String level = jobLevel != null ? jobLevel : "P7";
        os.setJobLevel(level);

        // ── 1. 市场数据 ──
        double marketPremium = context.get("marketPremium") instanceof Number
            ? ((Number) context.get("marketPremium")).doubleValue() : 0.05;
        int peerSalary = context.get("internalPeerSalary") instanceof Number
            ? ((Number) context.get("internalPeerSalary")).intValue() : 500000;
        boolean isSenior = level.contains("8") || level.contains("9") || level.contains("M");

        // ── 2. 薪酬区间 ──
        OfferStrategy.SuggestedRange range = buildRange(level, marketPremium, peerSalary);
        os.setSuggestedRange(range);

        // ── 3. 薪酬结构 ──
        os.setComponents(buildComponents(range, isSenior));

        // ── 4. 竞品场景模拟 ──
        os.setCompetingScenarios(buildScenarios(candidateName, level, range));

        // ── 5. 三方案对比 ──
        os.setStrategyOptions(buildOptions(level, range));

        // ── 6. 谈判策略（LLM 增强）──
        os.setNegotiationTips(buildTips(candidateName, jobTitle, level, range, isSenior));

        // ── 7. 风险矩阵 ──
        os.setRisks(buildRisks(isSenior, candidateName));

        // ── 8. 摘要 ──
        os.setStrategySummary(buildSummary(isSenior, level, range));
        os.setConfidence(0.72);

        return os;
    }

    // ──────────── 竞品场景 ────────────

    private List<Map<String, Object>> buildScenarios(String candidateName, String level, OfferStrategy.SuggestedRange range) {
        List<Map<String, Object>> scenarios = new ArrayList<>();

        // 场景 1：无竞品
        scenarios.add(Map.of(
            "scenario", "无竞品 Offer",
            "competitor", "无",
            "theirOffer", "—",
            "ourSuggested", formatRange(range),
            "winProbability", "85%",
            "recommendation", "标准出价，强调整体薪酬包 + 发展空间",
            "riskLevel", "LOW"
        ));

        // 场景 2：有大厂竞品
        int competitorOffer = (int) (range.getMid() * 1.15);
        scenarios.add(Map.of(
            "scenario", "大厂竞品 Offer",
            "competitor", "字节跳动 / 阿里",
            "theirOffer", formatMoney(competitorOffer) + " + 期权",
            "ourSuggested", formatMoney((int) (range.getMid() * 1.05)) + " + 签字费 + scope扩大",
            "winProbability", "55%",
            "recommendation", "不要跟竞品拼现金，强调业务 ownership 和晋升通道。签字费可作为灵活谈判工具。",
            "riskLevel", "HIGH"
        ));

        // 场景 3：有创业公司竞品
        scenarios.add(Map.of(
            "scenario", "创业公司竞品",
            "competitor", "Pre-IPO / 独角兽",
            "theirOffer", formatMoney((int) (range.getMid() * 0.8)) + " + 大量期权",
            "ourSuggested", formatMoney(range.getMid()) + " + 确定性的现金 + 适度期权",
            "winProbability", "70%",
            "recommendation", "强调现金确定性 vs 期权不确定性。如果候选人是风险偏好型，可增加期权部分。",
            "riskLevel", "MEDIUM"
        ));

        return scenarios;
    }

    // ──────────── 三方案对比 ────────────

    private List<Map<String, Object>> buildOptions(String level, OfferStrategy.SuggestedRange range) {
        return List.of(
            Map.of(
                "name", "保守方案",
                "totalComp", formatMoney((int) (range.getMid() * 0.9)),
                "baseSalary", formatMoney((int) (range.getMid() * 0.9 * 0.7)),
                "bonusPercent", "15%",
                "signingBonus", "0",
                "options", "无",
                "pros", "预算友好，低风险，适合标准候选人",
                "cons", "竞争力弱，可能流失高质量候选人",
                "targetCandidate", "匹配度中等的候选人"
            ),
            Map.of(
                "name", "标准方案",
                "totalComp", formatMoney(range.getMid()),
                "baseSalary", formatMoney((int) (range.getMid() * 0.7)),
                "bonusPercent", "20%",
                "signingBonus", formatMoney((int) (range.getMid() * 0.1)),
                "options", level.contains("8") ? "待定" : "无",
                "pros", "市场竞争力充足，预算可控",
                "cons", "对顶尖人才可能不够",
                "targetCandidate", "大多数合格候选人"
            ),
            Map.of(
                "name", "激进方案",
                "totalComp", formatMoney((int) (range.getMax() * 1.1)),
                "baseSalary", formatMoney((int) (range.getMax() * 1.1 * 0.7)),
                "bonusPercent", "25%",
                "signingBonus", formatMoney((int) (range.getMax() * 0.15)),
                "options", "有",
                "pros", "竞争力最强，可争夺顶尖人才",
                "cons", "预算压力大，可能引起内部薪酬倒挂",
                "targetCandidate", "关键岗位 / 稀缺人才"
            )
        );
    }

    // ──────────── 谈判建议 ────────────

    private List<String> buildTips(String candidateName, String jobTitle, String level,
                                    OfferStrategy.SuggestedRange range, boolean isSenior) {
        List<String> tips = new ArrayList<>();

        // 规则生成的建议
        tips.add("首轮出价放在中点附近（" + formatMoney(range.getMid()) + "），留 10-15% 谈判空间");
        tips.add("先探候选人期望，再进入具体数字。问'你理想的薪酬包是什么样的？'而不是'你要多少钱？'");

        if (isSenior) {
            tips.add("P8+ 候选人更看重 scope 和影响力，可用业务空间替代部分现金");
            tips.add("强调直接向 VP 汇报和带团队的机会——这些对高级人才比 5% 的薪资差异更有吸引力");
        }

        tips.add("签字费是灵活的谈判工具：可以一次性解决候选人犹豫，且不计入长期薪酬成本");

        // LLM 增强建议
        try {
            LlmChatRequest req = new LlmChatRequest();
            req.setSystemPrompt("你是 RecruitOS 的 Offer 谈判策略顾问。给出 2 条简洁实用的谈判建议（每条一句话）。");
            req.setUserPrompt(String.format(
                "候选人：%s，岗位：%s（%s），薪酬区间：%d-%d 万。" +
                "请给出 2 条针对性的谈判策略建议，考虑行业惯例和心理战术。",
                candidateName, jobTitle, level, range.getMin() / 10000, range.getMax() / 10000));
            req.setScenario("offer_negotiation");

            String llmResponse = llmClient.chat(req);
            if (llmResponse != null && !llmResponse.isEmpty()) {
                for (String line : llmResponse.split("[。\\n]")) {
                    line = line.replaceAll("^[\\d\\.\\-\\s]+", "").trim();
                    if (line.length() > 10) tips.add(line);
                }
            }
        } catch (Exception e) {
            log.debug("LLM negotiation tips unavailable, using rules-only");
        }

        return tips;
    }

    // ──────────── 风险 ────────────

    private List<OfferStrategy.RiskItem> buildRisks(boolean isSenior, String candidateName) {
        List<OfferStrategy.RiskItem> risks = new ArrayList<>();

        OfferStrategy.RiskItem r1 = new OfferStrategy.RiskItem();
        r1.setRisk("候选人当前薪酬可能高于预算上限");
        r1.setSeverity(isSenior ? "HIGH" : "MEDIUM");
        risks.add(r1);

        OfferStrategy.RiskItem r2 = new OfferStrategy.RiskItem();
        r2.setRisk("竞品公司薪酬竞争力强，存在被反挖风险");
        r2.setSeverity("HIGH");
        risks.add(r2);

        OfferStrategy.RiskItem r3 = new OfferStrategy.RiskItem();
        r3.setRisk("Offer 周期过长可能导致候选人流失");
        r3.setSeverity("MEDIUM");
        risks.add(r3);

        if (isSenior) {
            OfferStrategy.RiskItem r4 = new OfferStrategy.RiskItem();
            r4.setRisk("高级岗位入职后薪酬倒挂可能引发团队不满");
            r4.setSeverity("MEDIUM");
            risks.add(r4);
        }

        return risks;
    }

    // ──────────── 辅助 ────────────

    private OfferStrategy.SuggestedRange buildRange(String level, double premium, int peerSalary) {
        OfferStrategy.SuggestedRange r = new OfferStrategy.SuggestedRange();
        boolean isSenior = level.contains("8") || level.contains("9") || level.contains("M");
        int base = isSenior ? 600000 : 400000;
        base = (int) (base * (1 + premium));
        // 调整到不低于内部同级
        base = Math.max(base, peerSalary);
        r.setMin((int) (base * 0.85));
        r.setMid(base);
        r.setMax((int) (base * 1.25));
        r.setCurrency("CNY");
        return r;
    }

    private List<OfferStrategy.CompComponent> buildComponents(OfferStrategy.SuggestedRange range, boolean isSenior) {
        List<OfferStrategy.CompComponent> comps = new ArrayList<>();
        OfferStrategy.CompComponent c = new OfferStrategy.CompComponent();
        c.setType("base"); c.setAmount((int) (range.getMid() * 0.7)); c.setNote("月薪约 " + (range.getMid() * 0.7 / 12 / 10000) + "万");
        comps.add(c);
        c = new OfferStrategy.CompComponent();
        c.setType("bonus"); c.setAmount((int) (range.getMid() * 0.2)); c.setNote("目标奖金 20%");
        comps.add(c);
        c = new OfferStrategy.CompComponent();
        c.setType("signing"); c.setAmount((int) (range.getMid() * 0.1)); c.setNote("签字费（一次性）");
        comps.add(c);
        if (isSenior) {
            c = new OfferStrategy.CompComponent();
            c.setType("options"); c.setAmount(0); c.setNote("期权待定，可在谈判中作为长期杠杆");
            comps.add(c);
        }
        return comps;
    }

    private String buildSummary(boolean isSenior, String level, OfferStrategy.SuggestedRange range) {
        return isSenior
            ? level + " 高端岗位：以业务空间和技术挑战为核心吸引力，薪酬对标市场 P75，期权作为长期绑定"
            : level + " 标准岗位：薪酬对标市场中位数，强调团队文化和成长空间";
    }

    private String formatMoney(int amount) {
        if (amount >= 10000) return (amount / 10000) + "万";
        return amount + "元";
    }

    private String formatRange(OfferStrategy.SuggestedRange r) {
        return (r.getMin()/10000) + "-" + (r.getMax()/10000) + "万";
    }
}
