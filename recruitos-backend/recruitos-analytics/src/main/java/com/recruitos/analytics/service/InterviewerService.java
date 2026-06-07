package com.recruitos.analytics.service;

import com.recruitos.analytics.dto.InterviewerEfficiencyVO;
import com.recruitos.analytics.mapper.AnalyticsQueryMapper;
import com.recruitos.common.tenant.TenantContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class InterviewerService {

    @Resource
    private AnalyticsQueryMapper analyticsQueryMapper;

    public List<InterviewerEfficiencyVO> getInterviewerData() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) tenantId = 1L;

        List<Map<String, Object>> stats = analyticsQueryMapper.getInterviewerStats(tenantId);
        List<InterviewerEfficiencyVO> list = new ArrayList<>();

        for (Map<String, Object> row : stats) {
            InterviewerEfficiencyVO vo = new InterviewerEfficiencyVO();
            vo.setInterviewerName((String) row.get("interviewerName"));
            int interviewCount = ((Number) row.get("interviewCount")).intValue();
            vo.setInterviewCount(interviewCount);

            Object passObj = row.get("passCount");
            int passCount = passObj != null ? ((Number) passObj).intValue() : 0;
            vo.setPassCount(passCount);
            vo.setPassRate(interviewCount > 0 ? Math.round(passCount * 1000.0 / interviewCount) / 10.0 : 0);

            Object avgScoreObj = row.get("avgScore");
            double avgScore = avgScoreObj != null ? ((Number) avgScoreObj).doubleValue() : 0;
            vo.setAvgScore(Math.round(avgScore * 10.0) / 10.0);

            vo.setAvgDecisionDays(1.0); // Default since we don't track this yet

            list.add(vo);
        }

        return list;
    }
}
