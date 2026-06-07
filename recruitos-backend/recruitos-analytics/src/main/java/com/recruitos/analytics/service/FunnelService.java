package com.recruitos.analytics.service;

import com.recruitos.analytics.dto.FunnelStageVO;
import com.recruitos.analytics.dto.FunnelVO;
import com.recruitos.analytics.mapper.AnalyticsQueryMapper;
import com.recruitos.common.tenant.TenantContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class FunnelService {

    @Resource
    private AnalyticsQueryMapper analyticsQueryMapper;

    public FunnelVO getFunnelData(String dateFrom, String dateTo) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) tenantId = 1L;

        FunnelVO vo = new FunnelVO();
        vo.setDateFrom(dateFrom);
        vo.setDateTo(dateTo);

        // Get candidate counts by status
        List<Map<String, Object>> candidateStats = analyticsQueryMapper.countCandidatesByStatus(tenantId, dateFrom, dateTo);
        int newCount = 0, screeningCount = 0, interviewingCount = 0, offerCount = 0, onboardCount = 0, poolCount = 0;
        for (Map<String, Object> row : candidateStats) {
            String status = (String) row.get("status");
            int cnt = ((Number) row.get("cnt")).intValue();
            if ("NEW".equals(status)) newCount = cnt;
            else if ("SCREENING".equals(status)) screeningCount = cnt;
            else if ("INTERVIEWING".equals(status)) interviewingCount = cnt;
            else if ("OFFER".equals(status)) offerCount = cnt;
            else if ("ONBOARD".equals(status)) onboardCount = cnt;
            else if ("POOL".equals(status)) poolCount = cnt;
        }

        // Get interview counts by round
        List<Map<String, Object>> interviewStats = analyticsQueryMapper.countInterviewsByRound(tenantId, dateFrom, dateTo);
        int initialCount = 0, finalCount = 0;
        for (Map<String, Object> row : interviewStats) {
            String round = (String) row.get("round");
            int cnt = ((Number) row.get("cnt")).intValue();
            if ("INITIAL".equals(round)) initialCount = cnt;
            else if ("FINAL".equals(round)) finalCount = cnt;
        }

        // Get offer counts
        List<Map<String, Object>> offerStats = analyticsQueryMapper.countOffersByStatus(tenantId, dateFrom, dateTo);
        int offerSentCount = 0, offerAcceptedCount = 0;
        for (Map<String, Object> row : offerStats) {
            String status = (String) row.get("status");
            int cnt = ((Number) row.get("cnt")).intValue();
            if ("SENT".equals(status) || "APPROVED".equals(status)) offerSentCount += cnt;
            if ("ACCEPTED".equals(status)) offerAcceptedCount = cnt;
        }

        // Get onboard count
        int onboardTotal = analyticsQueryMapper.countOnboard(tenantId, dateFrom, dateTo);

        // Build funnel stages
        int totalCandidates = newCount + screeningCount + interviewingCount + offerCount + onboardCount + poolCount;
        if (totalCandidates == 0) totalCandidates = 1; // Avoid division by zero

        List<FunnelStageVO> stages = new ArrayList<>();
        stages.add(buildStage("简历筛选", totalCandidates, 100.0));
        stages.add(buildStage("初面", initialCount, totalCandidates > 0 ? round(initialCount * 100.0 / totalCandidates) : 0));
        stages.add(buildStage("复试", finalCount, initialCount > 0 ? round(finalCount * 100.0 / initialCount) : 0));
        stages.add(buildStage("Offer", offerSentCount, finalCount > 0 ? round(offerSentCount * 100.0 / finalCount) : 0));
        stages.add(buildStage("入职", onboardTotal, offerSentCount > 0 ? round(onboardTotal * 100.0 / offerSentCount) : 0));

        vo.setStages(stages);
        return vo;
    }

    private FunnelStageVO buildStage(String name, int count, double rate) {
        FunnelStageVO stage = new FunnelStageVO();
        stage.setStageName(name);
        stage.setCount(count);
        stage.setConversionRate(rate);
        return stage;
    }

    private double round(double val) {
        return Math.round(val * 10.0) / 10.0;
    }
}
