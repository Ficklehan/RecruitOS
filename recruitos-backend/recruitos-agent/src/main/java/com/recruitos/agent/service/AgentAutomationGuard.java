package com.recruitos.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recruitos.agent.entity.AgentAccount;
import com.recruitos.agent.entity.CampaignPlatformRun;
import com.recruitos.agent.entity.JobSourcingCampaign;
import com.recruitos.agent.mapper.CampaignPlatformRunMapper;
import com.recruitos.agent.mapper.JobSourcingCampaignMapper;
import com.recruitos.agent.rpa.PlaywrightManager;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared guards for RPA automation: account must be ACTIVE and campaigns must be RUNNING.
 */
@Service
public class AgentAutomationGuard {

    private static final Set<String> RUNNABLE = new HashSet<>(Arrays.asList("ACTIVE"));

    @Resource
    private CampaignPlatformRunMapper platformRunMapper;
    @Resource
    private JobSourcingCampaignMapper campaignMapper;
    @Resource
    private PlaywrightManager playwrightManager;

    public boolean isAccountRunnable(AgentAccount account) {
        return account != null && StringUtils.hasText(account.getStatus())
                && RUNNABLE.contains(account.getStatus().trim().toUpperCase());
    }

    public String normalizeAccountStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "ACTIVE";
        }
        String s = status.trim().toUpperCase();
        if ("INACTIVE".equals(s) || "OFF".equals(s) || "CLOSED".equals(s)) {
            return "DISABLED";
        }
        return s;
    }

    public void onAccountDisabled(Long accountId) {
        playwrightManager.closeContext(accountId);
        pauseRunsUsingAccount(accountId);
    }

    public void pauseRunsUsingAccount(Long accountId) {
        LambdaQueryWrapper<CampaignPlatformRun> w = new LambdaQueryWrapper<>();
        w.eq(CampaignPlatformRun::getPrimaryAccountId, accountId)
                .in(CampaignPlatformRun::getStatus, Arrays.asList("RUNNING", "PENDING"));
        for (CampaignPlatformRun run : platformRunMapper.selectList(w)) {
            run.setStatus("PAUSED");
            platformRunMapper.updateById(run);
            maybePauseCampaign(run.getCampaignId());
        }
    }

    private void maybePauseCampaign(Long campaignId) {
        LambdaQueryWrapper<CampaignPlatformRun> w = new LambdaQueryWrapper<>();
        w.eq(CampaignPlatformRun::getCampaignId, campaignId);
        List<CampaignPlatformRun> runs = platformRunMapper.selectList(w);
        boolean anyRunning = false;
        for (CampaignPlatformRun run : runs) {
            if ("RUNNING".equals(run.getStatus()) || "PENDING".equals(run.getStatus())) {
                anyRunning = true;
                break;
            }
        }
        if (!anyRunning) {
            JobSourcingCampaign campaign = campaignMapper.selectById(campaignId);
            if (campaign != null && "RUNNING".equals(campaign.getStatus())) {
                campaign.setStatus("PAUSED");
                campaignMapper.updateById(campaign);
            }
        }
    }
}
