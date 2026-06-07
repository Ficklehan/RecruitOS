package com.recruitos.analytics.service;

import com.recruitos.analytics.dto.CycleStageVO;
import com.recruitos.analytics.dto.CycleVO;
import com.recruitos.analytics.mapper.AnalyticsQueryMapper;
import com.recruitos.common.tenant.TenantContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class CycleService {

    @Resource
    private AnalyticsQueryMapper analyticsQueryMapper;

    public CycleVO getCycleData(String dateFrom, String dateTo) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) tenantId = 1L;

        CycleVO vo = new CycleVO();
        vo.setDateFrom(dateFrom);
        vo.setDateTo(dateTo);

        List<CycleStageVO> stages = new ArrayList<>();
        double totalDays = 0;

        // Demand approval cycle
        Double demandApprovalDays = analyticsQueryMapper.getAvgDemandApprovalDays(tenantId, dateFrom, dateTo);
        double d1 = demandApprovalDays != null ? demandApprovalDays : 0;
        stages.add(buildStage("需求审批", d1));
        totalDays += d1;

        // Interview cycle
        Double interviewDays = analyticsQueryMapper.getAvgInterviewCycleDays(tenantId, dateFrom, dateTo);
        double d2 = interviewDays != null ? interviewDays : 0;
        stages.add(buildStage("面试流程", d2));
        totalDays += d2;

        // Offer to onboard cycle
        Double offerOnboardDays = analyticsQueryMapper.getAvgOfferToOnboardDays(tenantId, dateFrom, dateTo);
        double d3 = offerOnboardDays != null ? offerOnboardDays : 0;
        stages.add(buildStage("Offer到入职", d3));
        totalDays += d3;

        vo.setTotalAvgCycleDays(Math.round(totalDays * 10.0) / 10.0);
        vo.setStages(stages);

        return vo;
    }

    private CycleStageVO buildStage(String name, double days) {
        CycleStageVO stage = new CycleStageVO();
        stage.setStageName(name);
        stage.setAvgDays(Math.round(days * 10.0) / 10.0);
        return stage;
    }
}
