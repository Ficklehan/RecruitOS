package com.recruitos.brain.engine;

import com.recruitos.brain.domain.OfferStrategy;
import org.springframework.stereotype.Component;
import java.util.*;

/** 触点6引擎：Offer谈判策略 */
@Component
public class OfferStrategyEngine {

    public OfferStrategy generate(Long candidateId, String candidateName, Long jobId, String jobTitle,
                                   String jobLevel, Map<String, Object> context) {
        OfferStrategy os = new OfferStrategy();
        os.setCandidateId(candidateId);
        os.setCandidateName(candidateName);
        os.setJobId(jobId);
        os.setJobTitle(jobTitle);
        os.setJobLevel(jobLevel);

        // 薪酬区间计算
        OfferStrategy.SuggestedRange range = new OfferStrategy.SuggestedRange();
        String level = jobLevel != null ? jobLevel : "P7";
        boolean isSenior = level.contains("8") || level.contains("9") || level.contains("M");

        int base = isSenior ? 600000 : 400000;
        if (context.get("marketPremium") instanceof Number) {
            base = (int) (base * (1 + ((Number) context.get("marketPremium")).doubleValue()));
        }
        range.setMin((int) (base * 0.85));
        range.setMid(base);
        range.setMax((int) (base * 1.25));
        range.setCurrency("CNY");
        os.setSuggestedRange(range);

        // 薪酬结构
        List<OfferStrategy.CompComponent> comps = new ArrayList<>();
        OfferStrategy.CompComponent c1 = new OfferStrategy.CompComponent();
        c1.setType("base"); c1.setAmount(range.getMid()); c1.setNote("月薪" + (range.getMid() / 12 / 10000) + "万");
        comps.add(c1);
        OfferStrategy.CompComponent c2 = new OfferStrategy.CompComponent();
        c2.setType("bonus"); c2.setAmount((int) (range.getMid() * 0.2)); c2.setNote("目标奖金20%");
        comps.add(c2);
        if (isSenior) {
            OfferStrategy.CompComponent c3 = new OfferStrategy.CompComponent();
            c3.setType("options"); c3.setAmount(0); c3.setNote("期权待定，可在谈判中作为杠杆");
            comps.add(c3);
        }
        os.setComponents(comps);

        // 谈判策略
        List<String> tips = new ArrayList<>();
        tips.add("先探候选人期望vs预算差距，再进入具体数字谈判");
        tips.add("若候选人有竞品Offer，强调业务空间和团队质量而非单纯加价");
        if (isSenior) tips.add("P8+候选人更看重scope和影响力，可用业务空间替代部分现金");
        tips.add("首轮出价建议在中点附近，留10-15%谈判空间");
        os.setNegotiationTips(tips);

        // 风险
        List<OfferStrategy.RiskItem> risks = new ArrayList<>();
        OfferStrategy.RiskItem r1 = new OfferStrategy.RiskItem();
        r1.setRisk("候选人当前薪酬可能高于预算上限"); r1.setSeverity("MEDIUM");
        risks.add(r1);
        if (isSenior) {
            OfferStrategy.RiskItem r2 = new OfferStrategy.RiskItem();
            r2.setRisk("竞品公司薪酬竞争力强，可能被反挖"); r2.setSeverity("HIGH");
            risks.add(r2);
        }
        os.setRisks(risks);

        os.setStrategySummary(isSenior
                ? "高端岗位策略：以业务空间和技术挑战为核心吸引力，薪酬对标市场P75分位，期权作为长期绑定工具"
                : "标准岗位策略：薪酬对标市场中位数，强调团队文化和成长空间");
        os.setConfidence(0.70);

        return os;
    }
}
