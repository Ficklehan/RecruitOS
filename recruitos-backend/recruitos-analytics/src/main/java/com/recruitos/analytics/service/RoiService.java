package com.recruitos.analytics.service;

import com.recruitos.analytics.dto.RoiChannelVO;
import com.recruitos.analytics.dto.RoiVO;
import com.recruitos.analytics.mapper.AnalyticsQueryMapper;
import com.recruitos.common.tenant.TenantContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RoiService {

    @Resource
    private AnalyticsQueryMapper analyticsQueryMapper;

    public RoiVO getRoiData(String dateFrom, String dateTo) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) tenantId = 1L;

        RoiVO vo = new RoiVO();
        vo.setDateFrom(dateFrom);
        vo.setDateTo(dateTo);

        List<Map<String, Object>> sourceStats = analyticsQueryMapper.countCandidatesBySource(tenantId, dateFrom, dateTo);

        List<RoiChannelVO> channels = new ArrayList<>();
        double totalCost = 0;
        int totalHires = 0;

        // Map source to channel name and estimated cost per hire
        for (Map<String, Object> row : sourceStats) {
            String source = (String) row.get("source");
            int count = ((Number) row.get("cnt")).intValue();

            RoiChannelVO channel = new RoiChannelVO();
            channel.setChannelName(mapSourceToName(source));
            // Estimate hires as ~10% of candidates for each source
            int hires = Math.max(1, count / 5);
            channel.setHires(hires);
            double costPerHire = estimateCostPerHire(source);
            channel.setCostPerHire(costPerHire);
            channel.setCost(hires * costPerHire);
            channel.setConversionRate(count > 0 ? Math.round(hires * 1000.0 / count) / 10.0 : 0);
            channels.add(channel);

            totalCost += channel.getCost();
            totalHires += hires;
        }

        vo.setChannels(channels);
        vo.setTotalCost(totalCost);
        vo.setTotalHires(totalHires);
        vo.setOverallCostPerHire(totalHires > 0 ? Math.round(totalCost / totalHires * 100.0) / 100.0 : 0.0);

        return vo;
    }

    private String mapSourceToName(String source) {
        if (source == null) return "未知";
        switch (source) {
            case "PLATFORM": return "招聘平台";
            case "REFERRAL": return "内部推荐";
            case "HEADHUNTER": return "猎头";
            case "DIRECT": return "直接投递";
            default: return source;
        }
    }

    private double estimateCostPerHire(String source) {
        if (source == null) return 1000;
        switch (source) {
            case "PLATFORM": return 1500;
            case "REFERRAL": return 500;
            case "HEADHUNTER": return 5000;
            case "DIRECT": return 300;
            default: return 1000;
        }
    }
}
