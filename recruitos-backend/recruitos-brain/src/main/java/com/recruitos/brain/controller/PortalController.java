package com.recruitos.brain.controller;

import com.recruitos.brain.client.CandidateClient;
import com.recruitos.brain.client.InterviewClient;
import com.recruitos.brain.client.JobClient;
import com.recruitos.brain.client.OfferClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * 候选人门户 API — 候选人查看自己的申请进度、面试安排、Offer 详情。
 */
@RestController
@RequestMapping("/api/portal")
public class PortalController {

    private static final Logger log = LoggerFactory.getLogger(PortalController.class);

    @Resource
    private CandidateClient candidateClient;

    @Resource
    private JobClient jobClient;

    @Resource
    private InterviewClient interviewClient;

    @Resource
    private OfferClient offerClient;

    @GetMapping("/application")
    public Map<String, Object> getApplication(@RequestParam(defaultValue = "1") Long candidateId) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            // 获取候选人信息
            Map<String, Object> candidate = candidateClient.getCandidate(candidateId);
            if (candidate == null) {
                result.put("error", "候选人不存在");
                return result;
            }

            // 获取关联岗位
            Long jobId = toLong(candidate.get("jobId"));
            Map<String, Object> job = jobId != null ? jobClient.getJob(jobId) : null;

            result.put("jobTitle", job != null ? job.getOrDefault("title", "未知岗位") : candidate.getOrDefault("title", "未知"));
            result.put("company", job != null ? job.getOrDefault("company", "XX科技") : "XX科技");
            result.put("appliedAt", candidate.getOrDefault("createdAt", ""));

            // 流程阶段
            String stage = String.valueOf(candidate.getOrDefault("pipelineStage", "PENDING"));
            result.put("currentStage", stage);
            result.put("stageLabel", stageToLabel(stage));

            // 时间线
            result.put("timeline", buildTimeline(candidate));

            // 面试安排
            List<Map<String, Object>> interviews = fetchInterviews(candidateId);
            result.put("upcomingInterviews", interviews);

            // Offer 信息
            result.put("offers", fetchOffers(candidateId));

        } catch (Exception e) {
            log.error("Portal application fetch failed for candidate {}", candidateId, e);
            result.put("error", "数据获取失败");
        }

        return result;
    }

    private List<Map<String, Object>> buildTimeline(Map<String, Object> candidate) {
        List<Map<String, Object>> timeline = new ArrayList<>();
        String stage = String.valueOf(candidate.getOrDefault("pipelineStage", "PENDING"));

        Map<String, String[]> stages = Map.of(
            "SOURCED", new String[]{"简历投递", "done", "简历筛选", "pending", "面试", "pending", "Offer", "pending", "入职", "pending"},
            "SCREENING", new String[]{"简历投递", "done", "简历筛选", "current", "面试", "pending", "Offer", "pending", "入职", "pending"},
            "CONTACTED", new String[]{"简历投递", "done", "简历筛选", "done", "面试", "current", "Offer", "pending", "入职", "pending"},
            "INTERVIEWING", new String[]{"简历投递", "done", "简历筛选", "done", "面试", "current", "Offer", "pending", "入职", "pending"},
            "EVALUATED", new String[]{"简历投递", "done", "简历筛选", "done", "面试", "done", "Offer", "current", "入职", "pending"},
            "OFFER", new String[]{"简历投递", "done", "简历筛选", "done", "面试", "done", "Offer", "current", "入职", "pending"},
            "HIRED", new String[]{"简历投递", "done", "简历筛选", "done", "面试", "done", "Offer", "done", "入职", "current"}
        );

        String[] flow = stages.getOrDefault(stage,
            new String[]{"简历投递", "done", "简历筛选", "pending", "面试", "pending", "Offer", "pending", "入职", "pending"});

        String[] labels = {"简历投递", "简历筛选", "面试", "Offer", "入职"};
        for (int i = 0; i < labels.length; i++) {
            Map<String, Object> step = new LinkedHashMap<>();
            step.put("title", labels[i]);
            step.put("status", flow[i * 2 + 1]);
            step.put("date", candidate.getOrDefault("createdAt", "—"));
            step.put("desc", i == 0 ? "已成功提交申请" : "");
            timeline.add(step);
        }
        return timeline;
    }

    private List<Map<String, Object>> fetchInterviews(Long candidateId) {
        try {
            List<Map<String, Object>> interviews = interviewClient.getByCandidate(candidateId);
            if (interviews == null) return Collections.emptyList();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> iv : interviews) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", iv.get("id"));
                item.put("type", iv.getOrDefault("round", "面试"));
                item.put("date", iv.getOrDefault("scheduledAt", "待确认"));
                item.put("interviewer", iv.getOrDefault("interviewerName", "面试官"));
                item.put("duration", iv.getOrDefault("duration", "60 分钟"));
                item.put("note", iv.getOrDefault("note", ""));
                result.add(item);
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> fetchOffers(Long candidateId) {
        try {
            List<Map<String, Object>> offers = offerClient.getOffersByCandidate(candidateId);
            if (offers == null || offers.isEmpty()) return Collections.emptyList();
            return offers;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String stageToLabel(String stage) {
        switch (stage) {
            case "SOURCED":
            case "SCREENING":
                return "简历筛选中";
            case "CONTACTED":
                return "已联系";
            case "INTERVIEWING":
                return "面试中";
            case "EVALUATED":
                return "评估中";
            case "OFFER":
                return "Offer 阶段";
            case "HIRED":
                return "已录用";
            default:
                return "处理中";
        }
    }

    private Long toLong(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        return null;
    }
}
