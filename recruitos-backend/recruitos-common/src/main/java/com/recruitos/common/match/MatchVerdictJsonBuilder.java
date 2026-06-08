package com.recruitos.common.match;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 保守档位匹配结论 JSON 构建（candidate / agent 模块共用）
 */
public final class MatchVerdictJsonBuilder {

    public static final String TIER_PRIORITY = "PRIORITY";
    public static final String TIER_SCREEN = "SCREEN";
    public static final String TIER_REVIEW = "REVIEW";
    public static final String TIER_PASS = "PASS";
    public static final String TIER_PENDING = "PENDING";

    private MatchVerdictJsonBuilder() {
    }

    public static String build(MatchVerdictCandidateSnapshot candidate, BigDecimal totalScore,
                               MatchVerdictRankInfo rank, ObjectMapper objectMapper) {
        return build(candidate, totalScore, rank, null, objectMapper);
    }

    public static String build(MatchVerdictCandidateSnapshot candidate, BigDecimal totalScore,
                               MatchVerdictRankInfo rank, String jobTagsJson, ObjectMapper objectMapper) {
        Map<String, Object> detail = new LinkedHashMap<>();
        double score = totalScore != null ? totalScore.doubleValue() : 0;
        MatchVerdictRankInfo rankInfo = rank != null ? rank : new MatchVerdictRankInfo();

        if (candidate == null || !candidate.isHasParsedResume()) {
            detail.put("status", TIER_PENDING);
            detail.put("label", "信息不足，暂无法判断");
            detail.put("modelScore", null);
            detail.put("confidence", "LOW");
            detail.put("pros", Collections.emptyList());
            detail.put("cons", Collections.singletonList("简历未解析或信息不足，暂无法给出可靠匹配结论"));
            detail.put("summary", "请先完成简历解析后再评估匹配度");
            detail.put("suggestedAction", "解析简历");
            detail.put("autoActionAllowed", false);
            return writeJson(detail, objectMapper);
        }

        List<String> pros = new ArrayList<>();
        List<String> cons = new ArrayList<>();

        if (candidate.getWorkYears() != null && candidate.getWorkYears() >= 3) {
            pros.add("工作经验 " + candidate.getWorkYears() + " 年，达到常见岗位要求");
        } else {
            cons.add("工作年限偏短，建议人工确认项目深度");
        }
        if (hasText(candidate.getEducation())) {
            if (candidate.getEducation().contains("硕士") || candidate.getEducation().contains("博士")) {
                pros.add("学历背景：" + candidate.getEducation());
            } else {
                pros.add("学历：" + candidate.getEducation());
            }
        } else {
            cons.add("学历信息缺失");
        }
        JdTagMatcher.JdTagMatchResult jdMatch = JdTagMatcher.match(
                jobTagsJson, combineSkillSources(candidate), objectMapper);
        if (jdMatch.hasTags()) {
            int requiredHits = jdMatch.getRequiredHitCount();
            int requiredTotal = jdMatch.getRequiredTotalCount();
            int preferredHits = jdMatch.getPreferredHitCount();
            int preferredTotal = jdMatch.getPreferredTotalCount();
            if (requiredTotal > 0) {
                if (requiredHits >= requiredTotal) {
                    pros.add(0, "必备要求已全部符合（" + requiredHits + "/" + requiredTotal + "）");
                } else if (requiredHits > 0) {
                    cons.add(0, "必备要求部分符合（" + requiredHits + "/" + requiredTotal + "），建议重点核实未符合项");
                } else {
                    cons.add(0, "必备要求暂未体现，建议人工确认或补充了解");
                }
            }
            if (preferredTotal > 0 && preferredHits > 0) {
                pros.add("加分项符合 " + preferredHits + "/" + preferredTotal);
            } else if (preferredTotal > 0 && requiredHits >= requiredTotal) {
                pros.add("加分项可后续了解（" + preferredHits + "/" + preferredTotal + "）");
            }
            if (requiredTotal == 0) {
                double hitRate = (double) jdMatch.getHitCount() / jdMatch.getTotalCount();
                if (hitRate >= 0.6) {
                    pros.add(0, "任职要求符合 " + jdMatch.getHitCount() + "/" + jdMatch.getTotalCount() + " 项");
                } else {
                    cons.add(0, "任职要求仅符合 " + jdMatch.getHitCount() + "/" + jdMatch.getTotalCount() + " 项，建议逐项核实");
                }
            }
        } else if (hasText(candidate.getTags())) {
            pros.add("简历已提取技能信息，可与任职要求对照");
        } else {
            cons.add("简历技能信息不足，建议完善简历或提取任职要求");
        }
        if (hasText(candidate.getCurrentCompany())) {
            pros.add("当前就职：" + candidate.getCurrentCompany());
        }

        String tier = resolveTier(score, rankInfo.getPercentile(), cons.size());
        String label = tierLabel(tier);

        detail.put("status", tier);
        detail.put("label", label);
        detail.put("modelScore", BigDecimal.valueOf(score).setScale(1, RoundingMode.HALF_UP));
        detail.put("confidence", score >= 55 ? "MEDIUM" : "LOW");
        detail.put("pros", pros.size() > 3 ? pros.subList(0, 3) : pros);
        detail.put("cons", cons.size() > 3 ? cons.subList(0, 3) : cons);
        detail.put("summary", buildSummary(label, rankInfo));
        detail.put("rankInJob", rankInfo.getRank());
        detail.put("totalInJob", rankInfo.getTotal());
        detail.put("percentile", rankInfo.getPercentile());
        detail.put("suggestedAction", suggestedAction(tier));
        detail.put("autoActionAllowed", false);

        Map<String, Object> breakdown = new LinkedHashMap<>();
        if (jdMatch.hasTags()) {
            breakdown.put("skillMatch", jdMatch.getSkillMatchScore());
            detail.put("jdTagMatches", jdMatch.getJdTagMatches());
            detail.put("jdTagHitRate", round2((double) jdMatch.getHitCount() / jdMatch.getTotalCount()));
        } else {
            breakdown.put("skillMatch", hasText(candidate.getTags()) ? 65 : 40);
        }
        breakdown.put("experienceMatch", candidate.getWorkYears() != null && candidate.getWorkYears() >= 3 ? 70 : 45);
        breakdown.put("educationMatch", hasText(candidate.getEducation()) ? 60 : 35);
        breakdown.put("resumeCompleteness", candidate.isHasParsedResume() ? 78 : 35);
        detail.put("breakdown", breakdown);

        return writeJson(detail, objectMapper);
    }

    public static MatchVerdictRankInfo computeRank(int total, int higherCount) {
        MatchVerdictRankInfo info = new MatchVerdictRankInfo();
        info.setTotal(total);
        if (total <= 0) {
            info.setRank(1);
            info.setPercentile(50);
            return info;
        }
        info.setRank(higherCount + 1);
        int percentile = (int) Math.round((double) info.getRank() / total * 100);
        info.setPercentile(Math.max(percentile, 1));
        return info;
    }

    private static String resolveTier(double score, int percentile, int conCount) {
        if (score < 45 || conCount >= 3) {
            return TIER_PASS;
        }
        if (percentile > 0 && percentile <= 15 && score >= 72) {
            return TIER_PRIORITY;
        }
        if (percentile > 0 && percentile <= 40 && score >= 58) {
            return TIER_SCREEN;
        }
        if (score >= 68) {
            return TIER_SCREEN;
        }
        if (score >= 52) {
            return TIER_REVIEW;
        }
        return TIER_PASS;
    }

    private static String tierLabel(String tier) {
        switch (tier) {
            case TIER_PRIORITY: return "建议优先联系";
            case TIER_SCREEN: return "建议安排初筛";
            case TIER_REVIEW: return "建议再了解一下";
            case TIER_PASS: return "暂不合适（本职位）";
            default: return "信息不足，暂无法判断";
        }
    }

    private static String suggestedAction(String tier) {
        switch (tier) {
            case TIER_PRIORITY: return "建议优先联系并安排初筛";
            case TIER_SCREEN: return "建议通过初筛，进入下一轮";
            case TIER_REVIEW: return "建议查看匹配评估后再决定";
            case TIER_PASS: return "建议标记为不合适；如需长期跟进可单独储备至人才库";
            default: return "请完善简历或任职要求后再评估";
        }
    }

    private static String buildSummary(String label, MatchVerdictRankInfo rank) {
        if (rank.getTotal() <= 0) {
            return label + "：本职位候选人样本较少";
        }
        return label + "：在本职位候选人中相对靠前（第 " + rank.getRank() + "/" + rank.getTotal() + " 位）";
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static String combineSkillSources(MatchVerdictCandidateSnapshot candidate) {
        StringBuilder sb = new StringBuilder();
        if (hasText(candidate.getTags())) {
            sb.append(candidate.getTags()).append(',');
        }
        if (hasText(candidate.getCurrentTitle())) {
            sb.append(candidate.getCurrentTitle()).append(',');
        }
        return sb.toString();
    }

    private static String writeJson(Map<String, Object> detail, ObjectMapper objectMapper) {
        try {
            ObjectMapper mapper = objectMapper != null ? objectMapper : new ObjectMapper();
            return mapper.writeValueAsString(detail);
        } catch (Exception e) {
            return "{}";
        }
    }
}
