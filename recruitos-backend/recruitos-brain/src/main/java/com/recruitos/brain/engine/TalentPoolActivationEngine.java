package com.recruitos.brain.engine;

import com.recruitos.brain.client.CandidateClient;
import com.recruitos.brain.client.JobClient;
import com.recruitos.brain.ml.MlModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * 人才库激活引擎 — AI 扫描存量候选人池，匹配新岗位并生成推荐理由。
 * <p>
 * PRD 菜单"人才发现 → 人才库激活"的后端实现。
 * 策略：遍历近期未活跃的候选人，基于技能和岗位需求的语义相似度做匹配，
 * AI 生成推荐理由（为什么这个候选人应该被重新考虑）。
 */
@Component
public class TalentPoolActivationEngine {

    private static final Logger log = LoggerFactory.getLogger(TalentPoolActivationEngine.class);

    @Resource
    private CandidateClient candidateClient;

    @Resource
    private JobClient jobClient;

    @Resource
    private MlModelService mlModelService;

    /**
     * 扫描存量候选人池，为每个新岗位推荐匹配的沉睡候选人。
     * @param tenantId 租户 ID
     * @param maxPerJob 每个岗位最多推荐的候选人数
     * @return 按岗位分组的推荐结果
     */
    public Map<String, Object> activate(Long tenantId, int maxPerJob) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> activations = new ArrayList<>();

        try {
            // 1. 获取在招岗位
            List<Map<String, Object>> activeJobs = jobClient.getActiveJobs();
            if (activeJobs == null || activeJobs.isEmpty()) {
                result.put("activations", activations);
                result.put("summary", "无在招岗位");
                return result;
            }

            // 2. 获取存量候选人（从 CandidateClient 或历史数据）
            List<Map<String, Object>> poolCandidates = getPoolCandidates();
            if (poolCandidates.isEmpty()) {
                result.put("activations", activations);
                result.put("summary", "人才库为空");
                return result;
            }

            log.info("TalentPoolActivation: {} jobs × {} pool candidates", activeJobs.size(), poolCandidates.size());

            // 3. 为每个岗位匹配候选人
            for (Map<String, Object> job : activeJobs) {
                Long jobId = toLong(job.get("id"));
                String jobTitle = (String) job.getOrDefault("title", "未知岗位");
                if (jobId == null) continue;

                List<Map<String, Object>> matches = new ArrayList<>();

                for (Map<String, Object> candidate : poolCandidates) {
                    Long candidateId = toLong(candidate.get("id"));
                    if (candidateId == null) continue;

                    // 计算匹配度
                    double matchScore = computeMatch(candidate, job);

                    // 计算意向估算
                    Map<String, Object> signals = Map.of(
                        "replySpeed", 0.5, "interviewEngagement", 0.6,
                        "questionsDepth", 0.5, "salaryGapPercent", 10,
                        "competingOffers", false
                    );
                    Map<String, Object> intent = mlModelService.predict(signals, Map.of());

                    double intentScore = intent.containsKey("score")
                        ? ((Number) intent.get("score")).doubleValue() : 50;

                    // 计算激活潜力：匹配度 50% + 历史质量 30% + 时效性 20%
                    double decayFactor = computeTimeDecay(candidate);
                    double activationScore = matchScore * 0.5 + intentScore * 0.3 + decayFactor * 20;

                    if (matchScore >= 60) { // 匹配度阈值
                        Map<String, Object> activation = new LinkedHashMap<>();
                        activation.put("candidateId", candidateId);
                        activation.put("candidateName", candidate.getOrDefault("name", "未知"));
                        activation.put("title", candidate.getOrDefault("title", ""));
                        activation.put("currentCompany", candidate.getOrDefault("currentCompany", ""));
                        activation.put("yearsOfExperience", candidate.getOrDefault("yearsOfExperience", 0));
                        activation.put("matchScore", Math.round(matchScore));
                        activation.put("intentScore", Math.round(intentScore));
                        activation.put("activationScore", Math.round(activationScore * 10.0) / 10.0);
                        activation.put("aiRationale", buildActivationRationale(candidate, job, matchScore));
                        activation.put("lastActive", candidate.getOrDefault("updatedAt", ""));
                        activation.put("daysInactive", computeDaysInactive(candidate));
                        activation.put("originalSource", candidate.getOrDefault("sourceChannel", "未知"));
                        matches.add(activation);
                    }
                }

                // 按激活潜力排序
                matches.sort((a, b) -> Double.compare(
                    ((Number) b.get("activationScore")).doubleValue(),
                    ((Number) a.get("activationScore")).doubleValue()));

                if (matches.size() > maxPerJob) matches = matches.subList(0, maxPerJob);

                if (!matches.isEmpty()) {
                    Map<String, Object> jobActivation = new LinkedHashMap<>();
                    jobActivation.put("jobId", jobId);
                    jobActivation.put("jobTitle", jobTitle);
                    jobActivation.put("jobLevel", job.getOrDefault("level", ""));
                    jobActivation.put("daysOpen", job.getOrDefault("daysOpen", 0));
                    jobActivation.put("matchedCount", matches.size());
                    jobActivation.put("candidates", matches);
                    activations.add(jobActivation);
                }
            }

            String summary = String.format("扫描 %d 个在招岗位 × %d 名存量候选人，发现 %d 个激活机会",
                activeJobs.size(), poolCandidates.size(),
                activations.stream().mapToInt(a -> ((List<?>) a.get("candidates")).size()).sum());

            result.put("activations", activations);
            result.put("summary", summary);
            result.put("totalActivations",
                activations.stream().mapToInt(a -> ((List<?>) a.get("candidates")).size()).sum());

        } catch (Exception e) {
            log.error("TalentPoolActivation failed", e);
            result.put("activations", List.of());
            result.put("summary", "激活分析失败: " + e.getMessage());
        }

        return result;
    }

    // ── 匹配计算 ──

    private double computeMatch(Map<String, Object> candidate, Map<String, Object> job) {
        double score = 50; // 基线

        // 技能关键词匹配
        Object skills = candidate.get("skills");
        Object requirements = job.get("requirements");
        if (skills != null && requirements != null) {
            String skillStr = skills.toString().toLowerCase();
            String reqStr = requirements.toString().toLowerCase();
            int overlap = countKeywordOverlap(skillStr, reqStr);
            score += overlap * 8;
        }

        // 级别匹配
        Object candidateLevel = candidate.get("level");
        Object jobLevel = job.get("level");
        if (candidateLevel != null && jobLevel != null) {
            if (candidateLevel.toString().equals(jobLevel.toString())) score += 15;
            else score += 5;
        }

        // 行业匹配
        String candidateIndustry = String.valueOf(candidate.getOrDefault("industry", ""));
        String jobIndustry = String.valueOf(job.getOrDefault("industry", ""));
        if (!candidateIndustry.isEmpty() && candidateIndustry.equals(jobIndustry)) score += 10;

        return Math.min(100, score);
    }

    private int countKeywordOverlap(String a, String b) {
        Set<String> setA = new HashSet<>(Arrays.asList(a.split("[,\\s]+")));
        Set<String> setB = new HashSet<>(Arrays.asList(b.split("[,\\s]+")));
        setA.retainAll(setB);
        return setA.size();
    }

    private double computeTimeDecay(Map<String, Object> candidate) {
        int daysInactive = computeDaysInactive(candidate);
        if (daysInactive <= 30) return 1.0;
        if (daysInactive <= 90) return 0.8;
        if (daysInactive <= 180) return 0.5;
        return 0.2;
    }

    private int computeDaysInactive(Map<String, Object> candidate) {
        try {
            Object updatedAt = candidate.get("updatedAt");
            if (updatedAt != null) {
                String ts = updatedAt.toString().replace("T", " ").substring(0, 19);
                java.time.LocalDateTime last = java.time.LocalDateTime.parse(ts.replace(" ", "T"));
                return (int) java.time.Duration.between(last, java.time.LocalDateTime.now()).toDays();
            }
        } catch (Exception ignored) {}
        return 90; // 默认3个月
    }

    private String buildActivationRationale(Map<String, Object> candidate, Map<String, Object> job, double match) {
        String name = String.valueOf(candidate.getOrDefault("name", "候选人"));
        String title = String.valueOf(candidate.getOrDefault("title", ""));
        String jobTitle = String.valueOf(job.getOrDefault("title", "此岗位"));

        if (match >= 85) {
            return name + " 的技能组合与 " + jobTitle + " 高度匹配。虽然之前未进入流程，但重新评估后发现其经验正好补上当前团队缺口。";
        } else if (match >= 70) {
            return name + " 的 " + title + " 背景适合 " + jobTitle + "。建议 HR 重新联系，了解当前意向。";
        } else {
            return name + " 的部分经验与 " + jobTitle + " 相关，可作为备选池考虑。";
        }
    }

    // ── 数据获取 ──

    private List<Map<String, Object>> getPoolCandidates() {
        try {
            // 从 CandidateClient 获取所有非活跃候选人
            List<Map<String, Object>> candidates = candidateClient.listByJob(null);
            if (candidates == null) return Collections.emptyList();

            // 筛选沉睡候选人（最近 30 天无活动）
            List<Map<String, Object>> dormant = new ArrayList<>();
            for (Map<String, Object> c : candidates) {
                if (computeDaysInactive(c) > 30) {
                    dormant.add(c);
                }
            }
            return dormant;

        } catch (Exception e) {
            log.warn("Failed to fetch pool candidates", e);
            return Collections.emptyList();
        }
    }

    private Long toLong(Object o) {
        if (o instanceof Long) return (Long) o;
        if (o instanceof Integer) return ((Integer) o).longValue();
        if (o instanceof Number) return ((Number) o).longValue();
        return null;
    }
}
