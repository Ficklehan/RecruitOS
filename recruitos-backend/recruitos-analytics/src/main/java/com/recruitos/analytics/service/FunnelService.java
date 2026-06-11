package com.recruitos.analytics.service;

import com.recruitos.analytics.dto.ChannelCompareVO;
import com.recruitos.analytics.dto.FunnelStageVO;
import com.recruitos.analytics.dto.FunnelVO;
import com.recruitos.analytics.mapper.AnalyticsQueryMapper;
import com.recruitos.common.tenant.TenantContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FunnelService {

    private static final String[] CHANNEL_SOURCES = {"PLATFORM", "REFERRAL", "HEADHUNTER"};

    @Resource
    private AnalyticsQueryMapper analyticsQueryMapper;

    public FunnelVO getFunnelData(String dateFrom, String dateTo) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            tenantId = 1L;
        }

        FunnelVO vo = new FunnelVO();
        vo.setDateFrom(dateFrom);
        vo.setDateTo(dateTo);
        vo.setSourcingStages(buildSourcingStages(tenantId, dateFrom, dateTo));
        vo.setStages(buildPipelineStages(tenantId, dateFrom, dateTo));
        vo.setChannels(buildChannelCompare(tenantId, dateFrom, dateTo));
        return vo;
    }

    private List<FunnelStageVO> buildSourcingStages(Long tenantId, String dateFrom, String dateTo) {
        Map<String, Object> stats = analyticsQueryMapper.sumCampaignStats(tenantId, dateFrom, dateTo);
        int searched = intVal(stats, "searched");
        int greeted = intVal(stats, "greeted");
        int resumes = intVal(stats, "resumes");
        int imported = intVal(stats, "imported");
        int base = Math.max(searched, 1);

        List<FunnelStageVO> stages = new ArrayList<>();
        stages.add(buildStage("平台检索", searched, 100.0));
        stages.add(buildStage("打招呼", greeted, round(greeted * 100.0 / base)));
        stages.add(buildStage("收简历", resumes, greeted > 0 ? round(resumes * 100.0 / greeted) : 0));
        stages.add(buildStage("纳入候选人", imported, resumes > 0 ? round(imported * 100.0 / resumes) : 0));
        return stages;
    }

    private List<FunnelStageVO> buildPipelineStages(Long tenantId, String dateFrom, String dateTo) {
        List<Map<String, Object>> candidateStats = analyticsQueryMapper.countCandidatesByStatus(tenantId, dateFrom, dateTo);
        int newCount = 0, screeningCount = 0, interviewingCount = 0, offerCount = 0, onboardCount = 0, poolCount = 0;
        for (Map<String, Object> row : candidateStats) {
            String status = (String) row.get("status");
            int cnt = ((Number) row.get("cnt")).intValue();
            if ("NEW".equals(status)) {
                newCount = cnt;
            } else if ("SCREENING".equals(status)) {
                screeningCount = cnt;
            } else if ("INTERVIEWING".equals(status)) {
                interviewingCount = cnt;
            } else if ("OFFER".equals(status)) {
                offerCount = cnt;
            } else if ("ONBOARD".equals(status)) {
                onboardCount = cnt;
            } else if ("POOL".equals(status)) {
                poolCount = cnt;
            }
        }

        List<Map<String, Object>> interviewStats = analyticsQueryMapper.countInterviewsByRound(tenantId, dateFrom, dateTo);
        int initialCount = 0, finalCount = 0;
        for (Map<String, Object> row : interviewStats) {
            String round = (String) row.get("round");
            int cnt = ((Number) row.get("cnt")).intValue();
            if ("INITIAL".equals(round)) {
                initialCount = cnt;
            } else if ("FINAL".equals(round)) {
                finalCount = cnt;
            }
        }

        List<Map<String, Object>> offerStats = analyticsQueryMapper.countOffersByStatus(tenantId, dateFrom, dateTo);
        int offerSentCount = 0;
        for (Map<String, Object> row : offerStats) {
            String status = (String) row.get("status");
            int cnt = ((Number) row.get("cnt")).intValue();
            if ("SENT".equals(status) || "APPROVED".equals(status)) {
                offerSentCount += cnt;
            }
        }

        int onboardTotal = analyticsQueryMapper.countOnboard(tenantId, dateFrom, dateTo);
        int totalCandidates = newCount + screeningCount + interviewingCount + offerCount + onboardCount + poolCount;
        if (totalCandidates == 0) {
            totalCandidates = 1;
        }

        List<FunnelStageVO> stages = new ArrayList<>();
        stages.add(buildStage("简历筛选", totalCandidates, 100.0));
        stages.add(buildStage("初面", initialCount, totalCandidates > 0 ? round(initialCount * 100.0 / totalCandidates) : 0));
        stages.add(buildStage("复试", finalCount, initialCount > 0 ? round(finalCount * 100.0 / initialCount) : 0));
        stages.add(buildStage("Offer", offerSentCount, finalCount > 0 ? round(offerSentCount * 100.0 / finalCount) : 0));
        stages.add(buildStage("入职", onboardTotal, offerSentCount > 0 ? round(onboardTotal * 100.0 / offerSentCount) : 0));
        return stages;
    }

    private List<ChannelCompareVO> buildChannelCompare(Long tenantId, String dateFrom, String dateTo) {
        Map<String, Integer> candidatesBySource = new HashMap<>();
        for (Map<String, Object> row : analyticsQueryMapper.countCandidatesBySource(tenantId, dateFrom, dateTo)) {
            candidatesBySource.put((String) row.get("source"), ((Number) row.get("cnt")).intValue());
        }
        Map<String, Integer> hiresBySource = new HashMap<>();
        for (Map<String, Object> row : analyticsQueryMapper.countHiresBySource(tenantId, dateFrom, dateTo)) {
            hiresBySource.put((String) row.get("source"), ((Number) row.get("hires")).intValue());
        }

        List<ChannelCompareVO> channels = new ArrayList<>();
        for (String source : CHANNEL_SOURCES) {
            int candidates = candidatesBySource.getOrDefault(source, 0);
            int hires = hiresBySource.getOrDefault(source, 0);
            ChannelCompareVO ch = new ChannelCompareVO();
            ch.setSource(source);
            ch.setChannelName(mapSourceToName(source));
            ch.setCandidates(candidates);
            ch.setHires(hires);
            ch.setConversionRate(candidates > 0 ? round(hires * 100.0 / candidates) : 0);
            channels.add(ch);
        }
        return channels;
    }

    private String mapSourceToName(String source) {
        if ("PLATFORM".equals(source)) {
            return "自招（平台）";
        }
        if ("REFERRAL".equals(source)) {
            return "内部推荐";
        }
        if ("HEADHUNTER".equals(source)) {
            return "猎头";
        }
        return source;
    }

    private int intVal(Map<String, Object> map, String key) {
        if (map == null || map.get(key) == null) {
            return 0;
        }
        return ((Number) map.get(key)).intValue();
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
