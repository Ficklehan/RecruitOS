package com.recruitos.brain.engine;

import com.recruitos.brain.domain.SkillTagProfile;
import com.recruitos.brain.domain.TalentDensity;
import org.springframework.stereotype.Component;
import java.util.*;

/** 触点8引擎：人才密度评估 — 基于真实技能标签体系 */
@Component
public class TalentDensityEngine {

    /**
     * 从技能标签数据评估团队人才密度。
     * @param orgId 组织ID
     * @param orgName 组织名称
     * @param teamData 包含 "members" (List<SkillTagProfile>) 和 "targets" (List<CapabilityTarget>)
     */
    @SuppressWarnings("unchecked")
    public TalentDensity assess(Long orgId, String orgName, Map<String, Object> teamData) {
        TalentDensity td = new TalentDensity();
        td.setOrgId(orgId);
        td.setOrgName(orgName);

        List<SkillTagProfile> members = (List<SkillTagProfile>) teamData.getOrDefault("members", Collections.emptyList());
        List<SkillTagProfile.CapabilityTarget> targets = (List<SkillTagProfile.CapabilityTarget>)
            teamData.getOrDefault("targets", buildDefaultTargets(members));

        // 能力热力图：对比目标vs实际
        List<TalentDensity.CapabilityHeatmap> heatmap = new ArrayList<>();
        double totalWeightedCoverage = 0;
        double totalWeight = 0;

        for (SkillTagProfile.CapabilityTarget target : targets) {
            int proficientCount = countProficientMembers(members, target.getCapability(), target.getTargetProficiency());
            target.setCurrentHeadcount(proficientCount);

            double coverage = target.getTargetHeadcount() > 0
                ? Math.min(1.0, (double) proficientCount / target.getTargetHeadcount())
                : (proficientCount > 0 ? 1.0 : 0);

            TalentDensity.CapabilityHeatmap ch = new TalentDensity.CapabilityHeatmap();
            ch.setCapability(target.getCapability());
            ch.setCurrentLevel(coverage);
            ch.setTargetLevel(1.0);
            ch.setStatus(coverage >= 0.85 ? "SUFFICIENT"
                : coverage >= 0.6 ? "ADEQUATE" : "GAP");
            heatmap.add(ch);

            totalWeightedCoverage += coverage * target.getWeight();
            totalWeight += target.getWeight();
        }
        td.setHeatmap(heatmap);

        // 密度评分
        double avgCoverage = totalWeight > 0 ? totalWeightedCoverage / totalWeight : 0.5;
        td.setDensityScore(Math.round(avgCoverage * 100.0) / 100.0);
        td.setDensityLevel(avgCoverage >= 0.85 ? "HIGH" : avgCoverage >= 0.6 ? "MEDIUM" : "LOW");

        // 招聘增量：从 GAP 能力中计算
        List<TalentDensity.RecruitingIncrement> increments = new ArrayList<>();
        for (SkillTagProfile.CapabilityTarget target : targets) {
            if (target.getCurrentHeadcount() < target.getTargetHeadcount()) {
                TalentDensity.RecruitingIncrement inc = new TalentDensity.RecruitingIncrement();
                inc.setCapability(target.getCapability());
                double contrib = (1.0 - (double) target.getCurrentHeadcount() / Math.max(target.getTargetHeadcount(), 1))
                    * target.getWeight();
                inc.setContribution(Math.round(contrib * 100.0) / 100.0);
                inc.setNewHireId(null);
                increments.add(inc);
            }
        }
        td.setIncrements(increments);

        // 流失冲击：找出只有1人覆盖的关键能力
        List<TalentDensity.AttritionImpact> impacts = new ArrayList<>();
        for (SkillTagProfile.CapabilityTarget target : targets) {
            if (target.getCurrentHeadcount() == 1 && target.getTargetHeadcount() >= 1 && target.getWeight() > 0.15) {
                TalentDensity.AttritionImpact ai = new TalentDensity.AttritionImpact();
                ai.setCapability(target.getCapability());
                ai.setImpact(target.getWeight());
                ai.setCritical(true);
                impacts.add(ai);
            }
        }
        td.setAttritionImpacts(impacts);

        // Bar Raiser判断
        if (avgCoverage >= 0.85) {
            td.setBarRaiserVerdict("团队整体密度较高，新招聘应继续Bar Raising标准");
        } else if (avgCoverage >= 0.6) {
            td.setBarRaiserVerdict("团队存在部分能力缺口，建议招聘时优先填补" +
                increments.stream().limit(2).map(TalentDensity.RecruitingIncrement::getCapability)
                .reduce((a, b) -> a + "、" + b).orElse("关键能力"));
        } else {
            td.setBarRaiserVerdict("团队存在关键能力缺口，建议先招聘核心能力方向，暂缓Bar Raising");
        }
        td.setConfidence(members.size() >= 5 ? 0.80 : 0.55);

        return td;
    }

    /**
     * 统计达到目标熟练度的成员数。
     */
    private int countProficientMembers(List<SkillTagProfile> members, String capability, int targetProficiency) {
        int count = 0;
        for (SkillTagProfile m : members) {
            for (SkillTagProfile.SkillTag tag : m.getTags()) {
                if (tag.getName().equalsIgnoreCase(capability) && tag.getProficiency() >= targetProficiency) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    /**
     * 当没有显式 targets 时，从成员标签中自动构建默认目标。
     */
    private List<SkillTagProfile.CapabilityTarget> buildDefaultTargets(List<SkillTagProfile> members) {
        // 聚合所有标签
        Map<String, SkillTagProfile.CapabilityTarget> targetMap = new LinkedHashMap<>();
        for (SkillTagProfile m : members) {
            for (SkillTagProfile.SkillTag tag : m.getTags()) {
                targetMap.computeIfAbsent(tag.getName(), k -> {
                    SkillTagProfile.CapabilityTarget t = new SkillTagProfile.CapabilityTarget();
                    t.setCapability(k);
                    t.setCategory(tag.getCategory());
                    t.setTargetProficiency(3);
                    t.setTargetHeadcount(Math.max(1, members.size() / 4));
                    t.setWeight(1.0);
                    return t;
                });
            }
        }
        // 标准化权重
        double sumW = targetMap.values().stream().mapToDouble(SkillTagProfile.CapabilityTarget::getWeight).sum();
        for (SkillTagProfile.CapabilityTarget t : targetMap.values()) {
            t.setWeight(t.getWeight() / Math.max(sumW, 1));
        }
        return new ArrayList<>(targetMap.values());
    }
}
