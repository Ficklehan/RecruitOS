package com.recruitos.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recruitos.agent.entity.CampaignCandidateTrace;
import com.recruitos.agent.mapper.CampaignCandidateTraceMapper;
import com.recruitos.common.exception.BizException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class CampaignQuotaGuard {

    private static final List<String> GREET_STATUSES = Arrays.asList(
            "GREETED", "MONITORING", "RESUME_READY", "PENDING_IMPORT", "IMPORTED");

    @Resource
    private CampaignCandidateTraceMapper traceMapper;

    public void assertGreetAllowed(Long campaignId, String platform, Map<String, Object> quotas) {
        if (quotas == null || quotas.isEmpty()) {
            return;
        }
        Object limitObj = quotas.get(platform);
        if (!(limitObj instanceof Number)) {
            return;
        }
        int limit = ((Number) limitObj).intValue();
        if (limit <= 0) {
            return;
        }
        int used = countTodayGreets(campaignId, platform);
        if (used >= limit) {
            throw new BizException(platform + " 今日打招呼配额已用尽（" + limit + "）");
        }
    }

    public boolean isQuotaExhausted(Long campaignId, String platform, Map<String, Object> quotas) {
        if (quotas == null || quotas.isEmpty()) {
            return false;
        }
        Object limitObj = quotas.get(platform);
        if (!(limitObj instanceof Number)) {
            return false;
        }
        int limit = ((Number) limitObj).intValue();
        return limit > 0 && countTodayGreets(campaignId, platform) >= limit;
    }

    private int countTodayGreets(Long campaignId, String platform) {
        LocalDateTime start = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LambdaQueryWrapper<CampaignCandidateTrace> w = new LambdaQueryWrapper<>();
        w.eq(CampaignCandidateTrace::getCampaignId, campaignId)
                .eq(CampaignCandidateTrace::getPlatform, platform)
                .in(CampaignCandidateTrace::getTraceStatus, GREET_STATUSES)
                .ge(CampaignCandidateTrace::getUpdatedAt, start);
        return traceMapper.selectCount(w).intValue();
    }
}
