package com.recruitos.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitos.agent.dto.*;
import com.recruitos.agent.entity.*;
import com.recruitos.agent.mapper.*;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SourcingCampaignService {

    @Resource
    private JobSourcingCampaignMapper campaignMapper;
    @Resource
    private CampaignPlatformRunMapper platformRunMapper;
    @Resource
    private CampaignCandidateTraceMapper traceMapper;
    @Resource
    private AgentAccountMapper accountMapper;
    @Resource
    private JobPositionReadMapper jobPositionReadMapper;
    @Resource
    private OpsPackReadMapper opsPackReadMapper;
    @Resource
    private CandidateImportMapper jobReadMapper;
    @Resource
    private CandidateImportService candidateImportService;
    @Resource
    private CampaignOrchestratorService orchestratorService;
    @Resource
    private ObjectMapper objectMapper;

    @Transactional
    public WorkflowVO createAndStart(WorkflowCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        String jobStatus = jobPositionReadMapper.selectStatus(dto.getJobId(), tenantId);
        if (!"ACTIVE".equals(jobStatus)) {
            throw new BizException("岗位需为 ACTIVE 状态才能启动寻源");
        }

        List<CampaignPlatformConfigDTO> configs = resolvePlatformConfigs(dto, tenantId);
        if (configs.isEmpty()) {
            throw new BizException("请至少选择一个已配置主账号的平台");
        }

        String mode = StringUtils.hasText(dto.getMode()) ? dto.getMode() : "SEMI_AUTO";
        Map<String, Object> activePack = resolveActiveOpsPack(dto.getJobId(), tenantId, dto.getOpsPackId());
        if (activePack == null) {
            throw new BizException("请先确认渠道运营包后再启动寻源");
        }

        String greetStrategy = StringUtils.hasText(dto.getGreetStrategy())
                ? dto.getGreetStrategy() : greetStrategyFromPack(activePack);
        if ("CARD_GREET".equals(greetStrategy) && !Boolean.TRUE.equals(dto.getCardGreetRiskAccepted())) {
            throw new BizException("CARD_GREET 模式须勾选风险提示");
        }

        JobSourcingCampaign campaign = new JobSourcingCampaign();
        campaign.setJobId(dto.getJobId());
        campaign.setOpsPackId(toLong(activePack.get("id")));
        campaign.setOpsPackVersion(toInt(activePack.get("version")));
        campaign.setName(StringUtils.hasText(dto.getName()) ? dto.getName()
                : jobReadMapper.selectJobTitle(dto.getJobId(), tenantId) + "-寻源");
        campaign.setMode(mode);
        campaign.setStatus("RUNNING");
        campaign.setResumeConfirmRequired(boolToInt(dto.getResumeConfirmRequired(), "SEMI_AUTO".equals(mode)));
        campaign.setPublishConfirmRequired(boolToInt(dto.getPublishConfirmRequired(), "SEMI_AUTO".equals(mode)));
        campaign.setStartedAt(LocalDateTime.now());

        Map<String, Object> config = new LinkedHashMap<>();
        List<String> keywords = dto.getKeywords() != null && !dto.getKeywords().isEmpty()
                ? dto.getKeywords() : keywordsFromOpsPack(activePack, dto.getJobId(), tenantId);
        config.put("keywords", keywords);
        config.put("dailyLimit", dto.getDailyLimit() != null ? dto.getDailyLimit() : dailyLimitFromPack(activePack));
        config.put("templateId", dto.getTemplateId());
        config.put("platformConfigs", configs);
        config.put("opsPackId", campaign.getOpsPackId());
        config.put("opsPackVersion", campaign.getOpsPackVersion());
        config.put("greetStrategy", greetStrategy);
        config.put("searchSource", StringUtils.hasText(dto.getSearchSource()) ? dto.getSearchSource() : "RECOMMEND");
        config.put("screeningProfile", nestedPack(activePack, "screeningProfile"));
        config.put("rechatPolicy", nestedPack(activePack, "rechatPolicy"));
        Object quotas = dto.getPlatformQuotas() != null && !dto.getPlatformQuotas().isEmpty()
                ? dto.getPlatformQuotas() : nestedPack(activePack, "platformQuotas");
        config.put("platformQuotas", quotas);
        config.put("communicationProfile", nestedPack(activePack, "communicationProfile"));
        try {
            campaign.setConfigJson(objectMapper.writeValueAsString(config));
            campaign.setStatsJson(objectMapper.writeValueAsString(emptyStats()));
        } catch (Exception e) {
            throw new BizException("配置序列化失败");
        }
        campaignMapper.insert(campaign);

        for (CampaignPlatformConfigDTO pc : configs) {
            CampaignPlatformRun run = new CampaignPlatformRun();
            run.setId(IdWorker.getId());
            run.setTenantId(tenantId);
            run.setCampaignId(campaign.getId());
            run.setPlatform(pc.getPlatform());
            run.setPrimaryAccountId(pc.getPrimaryAccountId());
            try {
                run.setAuxiliaryAccountIds(objectMapper.writeValueAsString(
                        pc.getAuxiliaryAccountIds() != null ? pc.getAuxiliaryAccountIds() : Collections.emptyList()));
            } catch (Exception e) {
                run.setAuxiliaryAccountIds("[]");
            }
            run.setCurrentStep(CampaignOrchestratorService.STEP_LOGIN);
            run.setStatus("PENDING");
            run.setCreatedAt(LocalDateTime.now());
            run.setUpdatedAt(LocalDateTime.now());
            platformRunMapper.insert(run);
        }

        return getWorkflowDetail(campaign.getId());
    }

    public List<WorkflowVO> listWorkflows(Long jobId) {
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<JobSourcingCampaign> w = new LambdaQueryWrapper<>();
        w.eq(JobSourcingCampaign::getTenantId, tenantId);
        if (jobId != null) {
            w.eq(JobSourcingCampaign::getJobId, jobId);
        }
        w.orderByDesc(JobSourcingCampaign::getCreatedAt);
        List<WorkflowVO> list = new ArrayList<>();
        for (JobSourcingCampaign c : campaignMapper.selectList(w)) {
            list.add(toWorkflowVO(c, false));
        }
        return list;
    }

    public WorkflowVO getWorkflowDetail(Long id) {
        JobSourcingCampaign c = requireCampaign(id);
        orchestratorService.refreshCampaignStats(c);
        c = campaignMapper.selectById(id);
        return toWorkflowVO(c, true);
    }

    public WorkflowVO.WorkflowStatsVO getStats(Long id) {
        WorkflowVO vo = getWorkflowDetail(id);
        return vo.getStats();
    }

    public WorkflowVO pause(Long id) {
        JobSourcingCampaign c = requireCampaign(id);
        c.setStatus("PAUSED");
        campaignMapper.updateById(c);
        LambdaQueryWrapper<CampaignPlatformRun> w = new LambdaQueryWrapper<>();
        w.eq(CampaignPlatformRun::getCampaignId, id);
        for (CampaignPlatformRun run : platformRunMapper.selectList(w)) {
            run.setStatus("PAUSED");
            platformRunMapper.updateById(run);
        }
        return getWorkflowDetail(id);
    }

    public WorkflowVO resume(Long id) {
        JobSourcingCampaign c = requireCampaign(id);
        c.setStatus("RUNNING");
        campaignMapper.updateById(c);
        LambdaQueryWrapper<CampaignPlatformRun> w = new LambdaQueryWrapper<>();
        w.eq(CampaignPlatformRun::getCampaignId, id);
        for (CampaignPlatformRun run : platformRunMapper.selectList(w)) {
            if (!"FAILED".equals(run.getStatus())) {
                run.setStatus("RUNNING");
                platformRunMapper.updateById(run);
            }
        }
        return getWorkflowDetail(id);
    }

    public int pauseAllRunning() {
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<JobSourcingCampaign> cw = new LambdaQueryWrapper<>();
        cw.eq(JobSourcingCampaign::getTenantId, tenantId)
                .eq(JobSourcingCampaign::getStatus, "RUNNING");
        int count = 0;
        for (JobSourcingCampaign c : campaignMapper.selectList(cw)) {
            pause(c.getId());
            count++;
        }
        return count;
    }

    public List<CampaignCandidateTraceVO> listTraces(Long campaignId, String status) {
        JobSourcingCampaign campaign = requireCampaign(campaignId);
        LambdaQueryWrapper<CampaignCandidateTrace> w = new LambdaQueryWrapper<>();
        w.eq(CampaignCandidateTrace::getCampaignId, campaignId);
        if (StringUtils.hasText(status)) {
            w.eq(CampaignCandidateTrace::getTraceStatus, status);
        }
        w.orderByDesc(CampaignCandidateTrace::getUpdatedAt);
        List<CampaignCandidateTraceVO> list = new ArrayList<>();
        for (CampaignCandidateTrace t : traceMapper.selectList(w)) {
            list.add(toTraceVO(t, campaign.getJobId(), campaign.getTenantId()));
        }
        return list;
    }

    private List<CampaignPlatformConfigDTO> resolvePlatformConfigs(WorkflowCreateDTO dto, Long tenantId) {
        if (dto.getPlatformConfigs() != null && !dto.getPlatformConfigs().isEmpty()) {
            return dto.getPlatformConfigs();
        }
        List<CampaignPlatformConfigDTO> list = new ArrayList<>();
        if (dto.getPlatforms() == null) {
            return list;
        }
        for (String platform : dto.getPlatforms()) {
            CampaignPlatformConfigDTO pc = new CampaignPlatformConfigDTO();
            pc.setPlatform(platform);
            AgentAccount primary = pickPrimaryAccount(tenantId, platform);
            if (primary == null) {
                throw new BizException("平台 " + platform + " 无可用主账号");
            }
            pc.setPrimaryAccountId(primary.getId());
            list.add(pc);
        }
        return list;
    }

    private AgentAccount pickPrimaryAccount(Long tenantId, String platform) {
        LambdaQueryWrapper<AgentAccount> w = new LambdaQueryWrapper<>();
        w.eq(AgentAccount::getTenantId, tenantId)
                .eq(AgentAccount::getPlatform, platform)
                .eq(AgentAccount::getStatus, "ACTIVE")
                .orderByDesc(AgentAccount::getHealthScore);
        List<AgentAccount> accounts = accountMapper.selectList(w);
        return accounts.isEmpty() ? null : accounts.get(0);
    }

    private Map<String, Object> resolveActiveOpsPack(Long jobId, Long tenantId, Long opsPackId) {
        if (opsPackId != null) {
            Map<String, Object> pack = opsPackReadMapper.selectById(jobId, opsPackId, tenantId);
            if (pack != null && "ACTIVE".equals(String.valueOf(pack.get("status")))) {
                return pack;
            }
            throw new BizException("指定的运营包不存在或未确认生效");
        }
        return opsPackReadMapper.selectActive(jobId, tenantId);
    }

    @SuppressWarnings("unchecked")
    private List<String> keywordsFromOpsPack(Map<String, Object> pack, Long jobId, Long tenantId) {
        try {
            if (pack != null && pack.get("packJson") != null) {
                Map<String, Object> body = objectMapper.readValue(pack.get("packJson").toString(),
                        new TypeReference<Map<String, Object>>() {});
                Object kw = body.get("searchKeywords");
                if (kw instanceof List) {
                    return (List<String>) kw;
                }
            }
        } catch (Exception ignored) {
        }
        return defaultKeywords(jobId, tenantId);
    }

    private int dailyLimitFromPack(Map<String, Object> pack) {
        try {
            if (pack != null && pack.get("packJson") != null) {
                Map<String, Object> body = objectMapper.readValue(pack.get("packJson").toString(),
                        new TypeReference<Map<String, Object>>() {});
                Object quotas = body.get("platformQuotas");
                if (quotas instanceof Map) {
                    Map<?, ?> q = (Map<?, ?>) quotas;
                    int sum = 0;
                    for (Object v : q.values()) {
                        if (v instanceof Number) {
                            sum += ((Number) v).intValue();
                        }
                    }
                    if (sum > 0) {
                        return sum;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return 50;
    }

    private String greetStrategyFromPack(Map<String, Object> pack) {
        try {
            if (pack != null && pack.get("packJson") != null) {
                Map<String, Object> body = objectMapper.readValue(pack.get("packJson").toString(),
                        new TypeReference<Map<String, Object>>() {});
                Object gs = body.get("greetStrategy");
                if (gs != null) {
                    return gs.toString();
                }
            }
        } catch (Exception ignored) {
        }
        return "SCREEN_THEN_GREET";
    }

    private Object nestedPack(Map<String, Object> pack, String key) {
        try {
            if (pack != null && pack.get("packJson") != null) {
                Map<String, Object> body = objectMapper.readValue(pack.get("packJson").toString(),
                        new TypeReference<Map<String, Object>>() {});
                return body.get(key);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        return Long.parseLong(v.toString());
    }

    private Integer toInt(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        return Integer.parseInt(v.toString());
    }

    private List<String> defaultKeywords(Long jobId, Long tenantId) {
        String tags = jobReadMapper.selectJobTags(jobId, tenantId);
        if (StringUtils.hasText(tags)) {
            return Arrays.asList(tags.split("[,，、\\s]+"));
        }
        String title = jobReadMapper.selectJobTitle(jobId, tenantId);
        return Collections.singletonList(title != null ? title : "工程师");
    }

    private WorkflowVO toWorkflowVO(JobSourcingCampaign c, boolean withDetails) {
        WorkflowVO vo = new WorkflowVO();
        vo.setId(c.getId());
        vo.setName(c.getName());
        vo.setJobId(c.getJobId());
        vo.setJobTitle(jobReadMapper.selectJobTitle(c.getJobId(), c.getTenantId()));
        vo.setMode(c.getMode());
        vo.setStatus(c.getStatus());
        vo.setStartedAt(c.getStartedAt());
        vo.setCreatedAt(c.getCreatedAt());

        try {
            if (StringUtils.hasText(c.getConfigJson())) {
                Map<String, Object> cfg = objectMapper.readValue(c.getConfigJson(), new TypeReference<Map<String, Object>>() {});
                Object pcs = cfg.get("platformConfigs");
                if (pcs != null) {
                    vo.setPlatformConfigs(objectMapper.convertValue(pcs, new TypeReference<List<CampaignPlatformConfigDTO>>() {}));
                    for (CampaignPlatformConfigDTO pc : vo.getPlatformConfigs()) {
                        vo.getPlatforms().add(pc.getPlatform());
                    }
                }
            }
            if (StringUtils.hasText(c.getStatsJson())) {
                vo.setStats(objectMapper.readValue(c.getStatsJson(), WorkflowVO.WorkflowStatsVO.class));
            }
        } catch (Exception ignored) {
        }
        if (vo.getStats() == null) {
            vo.setStats(new WorkflowVO.WorkflowStatsVO());
        }

        if (withDetails) {
            LambdaQueryWrapper<CampaignPlatformRun> rw = new LambdaQueryWrapper<>();
            rw.eq(CampaignPlatformRun::getCampaignId, c.getId());
            for (CampaignPlatformRun run : platformRunMapper.selectList(rw)) {
                vo.getPlatformRuns().add(toRunVO(run));
            }
            for (CampaignCandidateTraceVO t : listTraces(c.getId(), null)) {
                if (Arrays.asList("PENDING_GREET_CONFIRM", "PENDING_IMPORT", "PUBLISH_PENDING").contains(t.getTraceStatus())) {
                    vo.getPendingActions().add(t);
                }
            }
            // legacy stats shape for AgentWorkflow.vue
            vo.getStats().setGreeted(vo.getStats().getGreeted());
        }
        return vo;
    }

    private CampaignPlatformRunVO toRunVO(CampaignPlatformRun run) {
        CampaignPlatformRunVO vo = new CampaignPlatformRunVO();
        vo.setId(run.getId());
        vo.setPlatform(run.getPlatform());
        vo.setPrimaryAccountId(run.getPrimaryAccountId());
        AgentAccount acc = accountMapper.selectById(run.getPrimaryAccountId());
        if (acc != null) {
            vo.setPrimaryAccountName(acc.getAccountName());
        }
        vo.setCurrentStep(run.getCurrentStep());
        vo.setStatus(run.getStatus());
        vo.setPlatformJobUrl(run.getPlatformJobUrl());
        vo.setErrorMessage(run.getErrorMessage());
        return vo;
    }

    private CampaignCandidateTraceVO toTraceVO(CampaignCandidateTrace t, Long jobId, Long tenantId) {
        CampaignCandidateTraceVO vo = new CampaignCandidateTraceVO();
        vo.setId(t.getId());
        vo.setPlatform(t.getPlatform());
        vo.setCandidateName(t.getCandidateName());
        vo.setPhone(t.getPhone());
        vo.setTraceStatus(t.getTraceStatus());
        vo.setSkipReason(t.getSkipReason());
        vo.setScreenStage(t.getScreenStage());
        vo.setSkipReasonSummary(summarizeSkipReason(t));
        vo.setGreetStrategyApplied(t.getGreetStrategyApplied());
        vo.setOpsPackVersion(t.getOpsPackVersion());
        vo.setLockedByAccountId(t.getLockedByAccountId());
        if (t.getLockedByAccountId() != null) {
            AgentAccount a = accountMapper.selectById(t.getLockedByAccountId());
            if (a != null) {
                vo.setLockedByAccountName(a.getAccountName());
            }
        }
        vo.setMatchScore(t.getMatchScore());
        if (jobId != null && tenantId != null && t.getMatchScore() != null) {
            String detail = null;
            if (t.getCandidateId() != null) {
                detail = jobReadMapper.selectMatchDetail(tenantId, t.getCandidateId(), jobId);
            }
            if (!StringUtils.hasText(detail)) {
                com.recruitos.agent.platform.PlatformCandidate pc = new com.recruitos.agent.platform.PlatformCandidate();
                pc.setName(t.getCandidateName());
                pc.setPhone(t.getPhone());
                detail = candidateImportService.buildTraceMatchDetail(
                        tenantId, jobId, pc, t.getMatchScore(), t.getResumeId() != null);
            }
            vo.setMatchDetail(detail);
        }
        vo.setCandidateId(t.getCandidateId());
        vo.setResumeId(t.getResumeId());
        vo.setUpdatedAt(t.getUpdatedAt());
        return vo;
    }

    private JobSourcingCampaign requireCampaign(Long id) {
        Long tenantId = TenantContext.getTenantId();
        JobSourcingCampaign c = campaignMapper.selectById(id);
        if (c == null || !c.getTenantId().equals(tenantId)) {
            throw new BizException("寻源活动不存在");
        }
        return c;
    }

    private int boolToInt(Boolean v, boolean defaultTrue) {
        if (v == null) {
            return defaultTrue ? 1 : 0;
        }
        return Boolean.TRUE.equals(v) ? 1 : 0;
    }

    private Map<String, Integer> emptyStats() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("published", 0);
        m.put("searched", 0);
        m.put("locked", 0);
        m.put("greeted", 0);
        m.put("replied", 0);
        m.put("resumes", 0);
        m.put("imported", 0);
        m.put("pendingScreening", 0);
        m.put("duplicatesSkipped", 0);
        m.put("screenSkipped", 0);
        m.put("alerts", 0);
        return m;
    }

    private String summarizeSkipReason(CampaignCandidateTrace t) {
        if (StringUtils.hasText(t.getSkipReason())) {
            return t.getSkipReason();
        }
        if (!StringUtils.hasText(t.getSkipReasonJson())) {
            return null;
        }
        try {
            Map<String, Object> m = objectMapper.readValue(t.getSkipReasonJson(),
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            Object stage = m.get("stage");
            Object rule = m.get("rule");
            Object evidence = m.get("evidence");
            if (stage != null && rule != null) {
                return stage + " · " + rule + (evidence != null ? " (" + evidence + ")" : "");
            }
        } catch (Exception ignored) {
        }
        return t.getSkipReasonJson();
    }
}
