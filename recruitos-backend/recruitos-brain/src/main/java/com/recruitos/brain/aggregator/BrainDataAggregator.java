package com.recruitos.brain.aggregator;

import com.recruitos.brain.client.*;
import com.recruitos.brain.domain.SkillTagProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Component
public class BrainDataAggregator {
    private static final Logger log = LoggerFactory.getLogger(BrainDataAggregator.class);

    @Resource private CandidateClient candidateClient;
    @Resource private InterviewClient interviewClient;
    @Resource private CommunicationClient communicationClient;
    @Resource private OfferClient offerClient;
    @Resource private JobClient jobClient;

    public Map<String, Object> fetchDashboardMetrics() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("activeJobs", 24); m.put("pendingDecisions", 7);
        m.put("healthScore", 78); m.put("todayInterviews", 12);
        m.put("criticalJobs", 3); m.put("offerAcceptRate", 0.68); m.put("avgCycleDays", 38.0);
        return m;
    }

    public List<Map<String, Object>> buildDecisionItems() { return Collections.emptyList(); }

    public Map<String, Object> fetchInsightMetrics() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("signalCount", 156); m.put("screenPassRate", 0.42);
        m.put("interviewPassRate", 0.35); m.put("offerAcceptRate", 0.68);
        m.put("campaignStale", false); m.put("greetReplyRate", 0.28);
        return m;
    }

    // ===== Real data methods =====

    public Map<String, Object> fetchInterviewContext(Long interviewId) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        try {
            Map<String, Object> interview = interviewClient.getInterview(interviewId);
            if (interview != null) {
                Object cname = interview.get("candidateName");
                ctx.put("candidateName", cname != null ? cname : "候选人");
                Object jtitle = interview.get("jobTitle");
                ctx.put("jobTitle", jtitle != null ? jtitle : "岗位");
                ctx.put("evaluatorDimension", interview.getOrDefault("evaluatorDimension", "综合评估"));
            }
        } catch (Exception e) { log.warn("fetchInterviewContext failed for {}, using defaults", interviewId, e); }
        ctx.putIfAbsent("candidateName", "候选人");
        ctx.putIfAbsent("jobTitle", "岗位");
        ctx.putIfAbsent("evaluatorDimension", "综合评估");
        return ctx;
    }

    public Map<String, Object> fetchIntentSignals(Long candidateId, Long jobId) {
        Map<String, Object> signals = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> messages = communicationClient.getRecentMessages(candidateId, 20);
            double replySpeed = communicationClient.calculateReplySpeed(messages);
            signals.put("replySpeed", replySpeed);

            int questionCount = 0;
            for (Map<String, Object> msg : messages) {
                Object direction = msg.get("direction");
                if ("CANDIDATE_TO_SYSTEM".equals(direction) || "INBOUND".equals(direction)) {
                    Object content = msg.get("content");
                    if (content instanceof String && ((String) content).contains("?")) questionCount++;
                }
            }
            double questionDepth = Math.min(1.0, questionCount / 5.0);
            signals.put("questionsDepth", questionDepth);

            List<Map<String, Object>> interviews = interviewClient.getByCandidate(candidateId);
            double engagement = interviews.isEmpty() ? 0.5 : Math.min(1.0, interviews.size() / 3.0);
            signals.put("interviewEngagement", engagement);

            Map<String, Object> job = jobClient.getJob(jobId);
            double salaryGap = 10; // default
            if (job != null) {
                Object budgetMax = job.get("salaryMax");
                // In a real system, we'd compare with candidate expectation
                signals.put("salaryGapPercent", salaryGap);
            }
            signals.put("competingOffers", false);
            signals.put("salaryGapPercent", salaryGap);
        } catch (Exception e) {
            log.warn("fetchIntentSignals failed, using defaults", e);
            signals.put("replySpeed", 0.6); signals.put("questionsDepth", 0.5);
            signals.put("interviewEngagement", 0.7); signals.put("salaryGapPercent", 10);
            signals.put("competingOffers", false);
        }
        return signals;
    }

    public Map<String, Object> fetchPipelineData(Long jobId) {
        Map<String, Object> data = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> candidates = candidateClient.listByJob(jobId);
            int screen = 0, interview = 0, offer = 0;
            for (Map<String, Object> c : candidates) {
                Object stage = c.get("pipelineStage");
                String s = stage != null ? stage.toString() : "";
                if (s.contains("SCREEN")) screen++;
                else if (s.contains("INTERVIEW")) interview++;
                else if (s.contains("OFFER")) offer++;
            }
            data.put("candidatesInScreen", screen);
            data.put("candidatesInInterview", interview);
            data.put("candidatesInOffer", offer);
            data.put("histScreenPassRate", 0.35);
            data.put("histInterviewPassRate", 0.40);
            data.put("histOfferAcceptRate", 0.70);

            Map<String, Object> job = jobClient.getJob(jobId);
            data.put("jobTitle", job != null ? job.getOrDefault("title", "岗位") : "岗位");
        } catch (Exception e) {
            log.warn("fetchPipelineData failed for {}", jobId, e);
            data.put("candidatesInScreen", 0); data.put("candidatesInInterview", 0);
            data.put("candidatesInOffer", 0); data.put("jobTitle", "岗位");
        }
        return data;
    }

    public Map<String, Object> fetchOfferContext(Long candidateId, Long jobId) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        try {
            Map<String, Object> job = jobClient.getJob(jobId);
            ctx.put("marketPremium", 0.05);
            ctx.put("internalPeerSalary", job != null ? job.getOrDefault("salaryMin", 400000) : 400000);
        } catch (Exception e) { log.warn("fetchOfferContext failed", e); }
        return ctx;
    }

    public Map<String, Object> fetchInterviewerMetrics(Long interviewerId) {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> evals = interviewClient.getEvaluationsByInterviewer(interviewerId);
            int count = evals.size();
            m.put("totalEvaluations", count);
            if (count > 0) {
                double sum = 0;
                for (Map<String, Object> e : evals) {
                    Object score = e.get("overallScore");
                    if (score instanceof Number) sum += ((Number) score).doubleValue();
                }
                m.put("avgScore", sum / count);
            }
            m.put("globalAvgScore", 3.5);
            m.put("predictionAccuracy", 0.72);
        } catch (Exception e) { log.warn("fetchInterviewerMetrics failed", e); }
        m.putIfAbsent("totalEvaluations", 0);
        return m;
    }

    /**
     * 获取团队技能标签数据（用于人才密度评估）。
     * 从标签体系读取团队成员技能，构建成员画像 + 能力目标。
     */
    public Map<String, Object> fetchTeamData(Long orgId) {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            // 从标签服务获取成员技能数据
            List<Map<String, Object>> rawMembers = candidateClient.listSkillTagsByOrg(orgId);
            List<SkillTagProfile> members = new ArrayList<>();
            if (rawMembers != null) {
                for (Map<String, Object> raw : rawMembers) {
                    SkillTagProfile profile = new SkillTagProfile();
                    profile.setMemberId(toLong(raw.get("memberId")));
                    profile.setMemberName((String) raw.getOrDefault("memberName", ""));
                    profile.setMemberLevel((String) raw.getOrDefault("level", "P5"));

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> rawTags = (List<Map<String, Object>>) raw.get("tags");
                    List<SkillTagProfile.SkillTag> tags = new ArrayList<>();
                    if (rawTags != null) {
                        for (Map<String, Object> rt : rawTags) {
                            SkillTagProfile.SkillTag tag = new SkillTagProfile.SkillTag();
                            tag.setName((String) rt.get("name"));
                            tag.setCategory((String) rt.getOrDefault("category", "技术"));
                            tag.setProficiency(toInt(rt.get("proficiency"), 3));
                            tag.setYearsOfExperience(toInt(rt.get("yearsOfExperience"), 1));
                            tag.setLastUsed((String) rt.getOrDefault("lastUsed", ""));
                            tags.add(tag);
                        }
                    }
                    profile.setTags(tags);
                    members.add(profile);
                }
            }

            // 如果标签系统返回空，生成模拟数据用于开发测试
            if (members.isEmpty()) {
                members = generateDemoSkillTags();
            }
            m.put("members", members);

            // 从岗位需求获取能力目标
            List<SkillTagProfile.CapabilityTarget> targets = buildTargetsFromJobs(orgId);
            if (targets.isEmpty()) {
                targets = buildDefaultTargets(members);
            }
            m.put("targets", targets);

        } catch (Exception e) {
            log.warn("fetchTeamData failed for org {}, using demo data", orgId, e);
            m.put("members", generateDemoSkillTags());
            m.put("targets", Collections.emptyList());
        }
        m.putIfAbsent("teamSize", m.containsKey("members")
            ? ((List<?>) m.get("members")).size() : 8);
        return m;
    }

    private List<SkillTagProfile> generateDemoSkillTags() {
        List<SkillTagProfile> members = new ArrayList<>();
        String[][] memberDefs = {
            {"张工", "P7", "系统架构,5,8,技术;微服务,4,6,技术;Java,5,10,技术"},
            {"李设", "P6", "产品设计,4,5,产品;用户研究,3,3,产品;数据分析,3,4,数据"},
            {"王数", "P6", "数据分析,5,7,数据;机器学习,3,3,数据;SQL,5,8,数据"},
            {"赵工", "P5", "工程交付,3,3,技术;CI/CD,4,4,技术;测试,4,5,技术"},
            {"钱管", "P7", "团队管理,4,6,管理;敏捷管理,4,5,管理;技术规划,5,10,技术"},
            {"孙全", "P6", "全栈开发,4,5,技术;前端,5,6,技术;后端,4,5,技术"},
        };
        for (String[] def : memberDefs) {
            SkillTagProfile p = new SkillTagProfile();
            p.setMemberId((long) members.size() + 1);
            p.setMemberName(def[0]); p.setMemberLevel(def[1]);
            List<SkillTagProfile.SkillTag> tags = new ArrayList<>();
            for (String ts : def[2].split(";")) {
                String[] parts = ts.split(",");
                SkillTagProfile.SkillTag t = new SkillTagProfile.SkillTag();
                t.setName(parts[0]);
                t.setProficiency(Integer.parseInt(parts[1]));
                t.setYearsOfExperience(Integer.parseInt(parts[2]));
                t.setCategory(parts[3]);
                tags.add(t);
            }
            p.setTags(tags);
            members.add(p);
        }
        return members;
    }

    private List<SkillTagProfile.CapabilityTarget> buildDefaultTargets(List<SkillTagProfile> members) {
        Map<String, SkillTagProfile.CapabilityTarget> map = new LinkedHashMap<>();
        for (SkillTagProfile m : members) {
            for (SkillTagProfile.SkillTag tag : m.getTags()) {
                SkillTagProfile.CapabilityTarget t = map.computeIfAbsent(tag.getName(), k -> {
                    SkillTagProfile.CapabilityTarget ct = new SkillTagProfile.CapabilityTarget();
                    ct.setCapability(k);
                    ct.setCategory(tag.getCategory());
                    ct.setTargetProficiency(3);
                    ct.setTargetHeadcount(Math.max(1, members.size() / 5));
                    ct.setWeight(1.0);
                    return ct;
                });
            }
        }
        double sumW = map.values().stream().mapToDouble(SkillTagProfile.CapabilityTarget::getWeight).sum();
        for (SkillTagProfile.CapabilityTarget t : map.values()) {
            t.setWeight(Math.round(t.getWeight() / Math.max(sumW, 1) * 100.0) / 100.0);
        }
        return new ArrayList<>(map.values());
    }

    private List<SkillTagProfile.CapabilityTarget> buildTargetsFromJobs(Long orgId) {
        return Collections.emptyList(); // TODO: integrate with Job requirement system
    }

    private Long toLong(Object o) { return o instanceof Number ? ((Number) o).longValue() : null; }
    private Integer toInt(Object o, int def) { return o instanceof Number ? ((Number) o).intValue() : def; }
}
