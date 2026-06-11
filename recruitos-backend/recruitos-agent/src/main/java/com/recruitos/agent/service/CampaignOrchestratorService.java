package com.recruitos.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitos.agent.entity.*;
import com.recruitos.agent.mapper.*;
import com.recruitos.agent.platform.*;
import com.recruitos.agent.rpa.RpaProperties;
import com.recruitos.common.match.TagMatchScoreCalculator;
import com.recruitos.common.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CampaignOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(CampaignOrchestratorService.class);

    public static final String STEP_LOGIN = "LOGIN";
    public static final String STEP_PUBLISH = "PUBLISH";
    public static final String STEP_PUBLISH_PENDING = "PUBLISH_PENDING";
    public static final String STEP_SEARCH = "SEARCH";
    public static final String STEP_GREET = "GREET";
    public static final String STEP_MONITOR = "MONITOR";
    public static final String STEP_FETCH = "FETCH";
    public static final String STEP_ACTIVE = "ACTIVE";
    public static final String STEP_DONE = "DONE";

    @Resource
    private JobSourcingCampaignMapper campaignMapper;
    @Resource
    private CampaignPlatformRunMapper platformRunMapper;
    @Resource
    private CampaignCandidateTraceMapper traceMapper;
    @Resource
    private CandidateAcquireLockMapper lockMapper;
    @Resource
    private AgentAccountMapper accountMapper;
    @Resource
    private AgentBehaviorLogMapper behaviorLogMapper;
    @Resource
    private CandidateImportMapper jobReadMapper;
    @Resource
    private CandidateImportService importService;
    @Resource
    private PlatformAdapterRegistry adapterRegistry;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private AgentAutomationGuard automationGuard;
    @Resource
    private RpaProperties rpaProperties;
    @Resource
    private ScreeningEngine screeningEngine;
    @Resource
    private CampaignEvolutionEmitter evolutionEmitter;
    @Resource
    private ChannelStagingCandidateMapper stagingMapper;
    @Resource
    private GreetingComposer greetingComposer;
    @Resource
    private CampaignQuotaGuard quotaGuard;

    @Scheduled(fixedDelay = 4000)
    public void tick() {
        LambdaQueryWrapper<JobSourcingCampaign> w = new LambdaQueryWrapper<>();
        w.eq(JobSourcingCampaign::getStatus, "RUNNING");
        List<JobSourcingCampaign> campaigns = campaignMapper.selectList(w);
        for (JobSourcingCampaign campaign : campaigns) {
            try {
                TenantContext.setTenantId(campaign.getTenantId());
                processCampaign(campaign);
            } catch (Exception e) {
                log.warn("Campaign tick failed id={}: {}", campaign.getId(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    public void processCampaign(JobSourcingCampaign campaign) {
        LambdaQueryWrapper<CampaignPlatformRun> rw = new LambdaQueryWrapper<>();
        rw.eq(CampaignPlatformRun::getCampaignId, campaign.getId())
                .in(CampaignPlatformRun::getStatus, Arrays.asList("RUNNING", "PENDING"));
        List<CampaignPlatformRun> runs = platformRunMapper.selectList(rw);
        for (CampaignPlatformRun run : runs) {
            if ("PENDING".equals(run.getStatus())) {
                run.setStatus("RUNNING");
                run.setStartedAt(LocalDateTime.now());
                platformRunMapper.updateById(run);
            }
            if (!"RUNNING".equals(run.getStatus())) {
                continue;
            }
            AgentAccount primary = accountMapper.selectById(run.getPrimaryAccountId());
            if (!automationGuard.isAccountRunnable(primary)) {
                run.setStatus("PAUSED");
                run.setErrorMessage("主账号已停用，寻源已暂停");
                platformRunMapper.updateById(run);
                continue;
            }
            advancePlatformRun(campaign, run);
        }
        if (!hasRunnableRuns(campaign.getId())) {
            return;
        }
        processTraces(campaign);
        refreshCampaignStats(campaign);
        checkCampaignComplete(campaign);
    }

    private void advancePlatformRun(JobSourcingCampaign campaign, CampaignPlatformRun run) {
        String step = normalizeStep(run.getCurrentStep());
        PlatformAdapter adapter = adapterRegistry.get(run.getPlatform());
        AgentAccount primary = accountMapper.selectById(run.getPrimaryAccountId());
        if (!automationGuard.isAccountRunnable(primary)) {
            run.setStatus("PAUSED");
            run.setErrorMessage("主账号已停用");
            platformRunMapper.updateById(run);
            return;
        }
        if (primary == null) {
            run.setStatus("FAILED");
            run.setErrorMessage("Primary account not found");
            platformRunMapper.updateById(run);
            return;
        }

        List<String> keywords = readKeywords(campaign);
        String jobTitle = jobReadMapper.selectJobTitle(campaign.getJobId(), campaign.getTenantId());
        String jdText = jobReadMapper.selectJobJdText(campaign.getJobId(), campaign.getTenantId());

        switch (step) {
            case STEP_LOGIN:
                loginAllAccounts(run, adapter, primary);
                run.setCurrentStep(STEP_PUBLISH);
                platformRunMapper.updateById(run);
                break;
            case STEP_PUBLISH:
                if (Boolean.TRUE.equals(readPublishPending(campaign))) {
                    run.setCurrentStep(STEP_PUBLISH_PENDING);
                    platformRunMapper.updateById(run);
                    break;
                }
                doPublish(campaign, run, adapter, primary, jobTitle, jdText, keywords);
                run.setCurrentStep(STEP_SEARCH);
                platformRunMapper.updateById(run);
                break;
            case STEP_PUBLISH_PENDING:
                break;
            case STEP_SEARCH:
                if (!readRunFlag(run, "searchDone")) {
                    doSearch(campaign, run, adapter, primary, keywords, jobTitle);
                    writeRunFlag(run, "searchDone", true);
                }
                run.setCurrentStep(STEP_ACTIVE);
                platformRunMapper.updateById(run);
                break;
            case STEP_ACTIVE:
                if (isPlatformRunIdle(campaign.getId(), run.getId())) {
                    run.setCurrentStep(STEP_DONE);
                    run.setStatus("COMPLETED");
                    platformRunMapper.updateById(run);
                }
                break;
            default:
                break;
        }
    }

    private void loginAllAccounts(CampaignPlatformRun run, PlatformAdapter adapter, AgentAccount primary) {
        adapter.login(primary);
        logBehavior(run, primary.getId(), "LOGIN", primary.getPlatform(), "登录 " + primary.getAccountName(), true, null);
        for (Long auxId : parseAuxIds(run.getAuxiliaryAccountIds())) {
            AgentAccount aux = accountMapper.selectById(auxId);
            if (aux != null) {
                adapter.login(aux);
                logBehavior(run, aux.getId(), "LOGIN", aux.getPlatform(), "辅账号登录 " + aux.getAccountName(), true, null);
            }
        }
    }

    private void doPublish(JobSourcingCampaign campaign, CampaignPlatformRun run, PlatformAdapter adapter,
                           AgentAccount primary, String jobTitle, String jdText, List<String> keywords) {
        String url = adapter.publishJob(primary, jobTitle, jdText, keywords);
        run.setPlatformJobUrl(url);
        logBehavior(run, primary.getId(), "PUBLISH", run.getPlatform(), "发布岗位: " + jobTitle, true, null);
    }

    public void confirmPublish(Long campaignId) {
        JobSourcingCampaign campaign = requireCampaign(campaignId);
        LambdaQueryWrapper<CampaignPlatformRun> w = new LambdaQueryWrapper<>();
        w.eq(CampaignPlatformRun::getCampaignId, campaignId)
                .eq(CampaignPlatformRun::getCurrentStep, STEP_PUBLISH_PENDING);
        List<CampaignPlatformRun> runs = platformRunMapper.selectList(w);
        for (CampaignPlatformRun run : runs) {
            AgentAccount primary = accountMapper.selectById(run.getPrimaryAccountId());
            PlatformAdapter adapter = adapterRegistry.get(run.getPlatform());
            String jobTitle = jobReadMapper.selectJobTitle(campaign.getJobId(), campaign.getTenantId());
            String jdText = jobReadMapper.selectJobJdText(campaign.getJobId(), campaign.getTenantId());
            doPublish(campaign, run, adapter, primary, jobTitle, jdText, readKeywords(campaign));
            run.setCurrentStep(STEP_SEARCH);
            platformRunMapper.updateById(run);
        }
        campaign.setPublishConfirmRequired(0);
        campaignMapper.updateById(campaign);
    }

    private void doSearch(JobSourcingCampaign campaign, CampaignPlatformRun run, PlatformAdapter adapter,
                          AgentAccount primary, List<String> keywords, String jobTitle) {
        List<Long> accountIds = new ArrayList<>();
        accountIds.add(primary.getId());
        accountIds.addAll(parseAuxIds(run.getAuxiliaryAccountIds()));

        for (Long accountId : accountIds) {
            AgentAccount account = accountMapper.selectById(accountId);
            if (account == null) {
                continue;
            }
            List<PlatformCandidate> found = adapter.searchCandidates(account, keywords, 3, readSearchSource(campaign));
            logBehavior(run, accountId, "SEARCH", run.getPlatform(),
                    "搜索命中 " + found.size() + " 人", true, null);
            for (PlatformCandidate pc : found) {
                handleSearchHit(campaign, run, account, pc, jobTitle);
            }
        }
    }

    private void handleSearchHit(JobSourcingCampaign campaign, CampaignPlatformRun run,
                                 AgentAccount account, PlatformCandidate pc, String jobTitle) {
        String dedupKey = buildDedupKey(run.getPlatform(), pc);
        Long existing = importService.findExistingCandidateId(campaign.getTenantId(), pc.getPhone(), pc.getEmail());
        if (existing != null) {
            createSkippedTrace(campaign, run, account, pc, dedupKey, "ALREADY_IN_TALENT_POOL", null, null);
            logBehavior(run, account.getId(), "DUPLICATE_SKIPPED", run.getPlatform(),
                    pc.getName() + " 已在人才库", true, null);
            return;
        }

        LambdaQueryWrapper<CandidateAcquireLock> lockW = new LambdaQueryWrapper<>();
        lockW.eq(CandidateAcquireLock::getCampaignId, campaign.getId())
                .eq(CandidateAcquireLock::getDedupKey, dedupKey);
        if (lockMapper.selectCount(lockW) > 0) {
            createSkippedTrace(campaign, run, account, pc, dedupKey, "LOCKED_BY_OTHER_ACCOUNT", null, null);
            logBehavior(run, account.getId(), "DUPLICATE_SKIPPED", run.getPlatform(),
                    pc.getName() + " 已被其他账号锁定", true, null);
            return;
        }

        applyPlatformMatchScore(campaign, pc);

        Object screeningProfile = readScreeningProfile(campaign);
        String greetStrategy = readGreetStrategy(campaign);
        boolean cardGreet = "CARD_GREET".equals(greetStrategy);

        String jobTagsJson = jobReadMapper.selectJobTags(campaign.getJobId(), campaign.getTenantId());
        ScreeningEngine.ScreeningResult stage1 = screeningEngine.evaluateStage1(screeningProfile, pc, jobTagsJson, objectMapper);
        if (!stage1.isPassed()) {
            createScreenSkippedTrace(campaign, run, account, pc, dedupKey, stage1);
            emitScreenSignal(campaign, null, pc, stage1, false);
            logBehavior(run, account.getId(), "SCREEN_SKIP", run.getPlatform(),
                    pc.getName() + " 卡片筛选淘汰: " + stage1.getHumanReason(), true, null);
            return;
        }

        ScreeningEngine.ScreeningResult stage2 = stage1;
        if (!cardGreet) {
            stage2 = screeningEngine.evaluateStage2(screeningProfile, pc, jobTagsJson, objectMapper);
            if (!stage2.isPassed()) {
                createScreenSkippedTrace(campaign, run, account, pc, dedupKey, stage2);
                emitScreenSignal(campaign, null, pc, stage2, false);
                logBehavior(run, account.getId(), "SCREEN_SKIP", run.getPlatform(),
                        pc.getName() + " 简历筛选淘汰: " + stage2.getHumanReason(), true, null);
                return;
            }
        }

        if (quotaGuard.isQuotaExhausted(campaign.getId(), run.getPlatform(), readPlatformQuotas(campaign))) {
            run.setStatus("PAUSED");
            run.setErrorMessage(run.getPlatform() + " 今日打招呼配额已用尽");
            platformRunMapper.updateById(run);
            logBehavior(run, account.getId(), "QUOTA_EXCEEDED", run.getPlatform(),
                    "配额耗尽，平台运行已暂停", false, null);
            return;
        }

        CandidateAcquireLock lock = new CandidateAcquireLock();
        lock.setId(IdWorker.getId());
        lock.setTenantId(campaign.getTenantId());
        lock.setCampaignId(campaign.getId());
        lock.setDedupKey(dedupKey);
        lock.setPlatform(run.getPlatform());
        lock.setPlatformUserId(pc.getPlatformUserId());
        lock.setLockedByAccountId(account.getId());
        lock.setStatus("LOCKED");
        lock.setCreatedAt(LocalDateTime.now());
        lockMapper.insert(lock);

        CampaignCandidateTrace trace = newTrace(campaign, run, account, pc, dedupKey);
        trace.setScreenStage(cardGreet ? "CARD" : "PASSED");
        trace.setTraceStatus("LOCKED");
        trace.setLockedByAccountId(account.getId());
        trace.setOpsPackVersion(readOpsPackVersion(campaign));
        appendTimeline(trace, "SCREEN_PASS", cardGreet ? "卡片筛选通过(CARD_GREET)" : "两阶段筛选通过");
        traceMapper.insert(trace);

        emitScreenSignal(campaign, trace, pc, stage2, true);

        trace.setGreetStrategyApplied(greetStrategy);

        if ("COLLECT_ONLY".equals(greetStrategy)) {
            saveStagingCandidate(campaign, trace, pc);
            trace.setTraceStatus("STAGED");
            traceMapper.updateById(trace);
            logBehavior(run, account.getId(), "STAGE_COLLECT", run.getPlatform(),
                    pc.getName() + " 已入库待人工处理", true, null);
            return;
        }

        if ("PUBLISH_SEARCH_ONLY".equals(campaign.getMode())) {
            traceMapper.updateById(trace);
            return;
        }

        boolean semi = "SEMI_AUTO".equals(campaign.getMode());
        if (semi) {
            trace.setTraceStatus("PENDING_GREET_CONFIRM");
            traceMapper.updateById(trace);
        } else {
            sendGreet(campaign, run, account, trace, pc, jobTitle);
        }
    }

    private void processTraces(JobSourcingCampaign campaign) {
        LambdaQueryWrapper<CampaignCandidateTrace> w = new LambdaQueryWrapper<>();
        w.eq(CampaignCandidateTrace::getCampaignId, campaign.getId())
                .in(CampaignCandidateTrace::getTraceStatus,
                        Arrays.asList("GREETED", "MONITORING", "RESUME_READY", "PENDING_IMPORT"));
        List<CampaignCandidateTrace> traces = traceMapper.selectList(w);
        for (CampaignCandidateTrace trace : traces) {
            CampaignPlatformRun run = platformRunMapper.selectById(trace.getPlatformRunId());
            if (run == null) {
                continue;
            }
            PlatformAdapter adapter = adapterRegistry.get(trace.getPlatform());
            AgentAccount account = accountMapper.selectById(trace.getAccountId());
            if (!automationGuard.isAccountRunnable(account)) {
                continue;
            }
            PlatformCandidate pc = toPlatformCandidate(trace);
            String jobTitle = jobReadMapper.selectJobTitle(campaign.getJobId(), campaign.getTenantId());

            if ("GREETED".equals(trace.getTraceStatus())) {
                trace.setTraceStatus("MONITORING");
                traceMapper.updateById(trace);
                logBehavior(run, account.getId(), "MONITOR", trace.getPlatform(), "监控会话 " + trace.getCandidateName(), true, null);
            } else if ("MONITORING".equals(trace.getTraceStatus())) {
                if (adapter.hasResumeInChat(account, pc.getPlatformUserId())) {
                    trace.setTraceStatus("RESUME_READY");
                    appendTimeline(trace, "RESUME_RECEIVED", "收到简历");
                    traceMapper.updateById(trace);
                    evolutionEmitter.emitResumeReceived(campaign.getJobId(), campaign.getId(),
                            trace.getId(), trace.getCandidateId());
                    evolutionEmitter.emitCandidateReply(campaign.getJobId(), campaign.getId(),
                            trace.getId(), trace.getCandidateId());
                } else if (shouldRechat(campaign, trace)) {
                    sendRechat(campaign, run, account, trace, jobTitle);
                }
            } else if ("RESUME_READY".equals(trace.getTraceStatus())) {
                PlatformResume resume = adapter.fetchResume(account, pc.getPlatformUserId(), pc.getName());
                logBehavior(run, account.getId(), "FETCH_RESUME", trace.getPlatform(), "获取简历 " + trace.getCandidateName(), true, null);
                trace.setTraceStatus("PENDING_IMPORT");
                traceMapper.updateById(trace);
                maybeImport(campaign, trace, pc, resume);
            } else if ("PENDING_IMPORT".equals(trace.getTraceStatus())) {
                // wait confirm
            }
        }
    }

    public void confirmGreet(Long traceId) {
        CampaignCandidateTrace trace = traceMapper.selectById(traceId);
        if (trace == null) {
            return;
        }
        JobSourcingCampaign campaign = campaignMapper.selectById(trace.getCampaignId());
        CampaignPlatformRun run = platformRunMapper.selectById(trace.getPlatformRunId());
        AgentAccount account = accountMapper.selectById(trace.getAccountId());
        if (campaign == null || run == null || account == null) {
            return;
        }
        String jobTitle = jobReadMapper.selectJobTitle(campaign.getJobId(), campaign.getTenantId());
        sendGreet(campaign, run, account, trace, toPlatformCandidate(trace), jobTitle);
    }

    public void confirmImport(Long traceId) {
        CampaignCandidateTrace trace = traceMapper.selectById(traceId);
        if (trace == null || !"PENDING_IMPORT".equals(trace.getTraceStatus())) {
            return;
        }
        JobSourcingCampaign campaign = campaignMapper.selectById(trace.getCampaignId());
        PlatformAdapter adapter = adapterRegistry.get(trace.getPlatform());
        AgentAccount account = accountMapper.selectById(trace.getAccountId());
        if (account == null) {
            return;
        }
        PlatformResume resume = adapter.fetchResume(account, trace.getPlatformUserId(), trace.getCandidateName());
        doImport(campaign, trace, toPlatformCandidate(trace), resume);
    }

    private void sendGreet(JobSourcingCampaign campaign, CampaignPlatformRun run, AgentAccount account,
                           CampaignCandidateTrace trace, PlatformCandidate pc, String jobTitle) {
        PlatformAdapter adapter = adapterRegistry.get(run.getPlatform());
        quotaGuard.assertGreetAllowed(campaign.getId(), run.getPlatform(), readPlatformQuotas(campaign));
        String message = greetingComposer.composeGreeting(campaign.getJobId(), jobTitle, pc.getName(), "GREET", pc);
        adapter.sendGreeting(account, pc.getPlatformUserId(), pc.getName(), jobTitle, message);
        trace.setTraceStatus("GREETED");
        trace.setAccountId(account.getId());
        appendTimeline(trace, "GREET_SENT", "已发送打招呼");
        trace.setUpdatedAt(LocalDateTime.now());
        traceMapper.updateById(trace);
        evolutionEmitter.emitGreet(campaign.getJobId(), campaign.getId(), trace.getId(), trace.getCandidateId());
        logBehavior(run, account.getId(), "GREET", run.getPlatform(), "打招呼 " + pc.getName(), true, null);
    }

    private void sendRechat(JobSourcingCampaign campaign, CampaignPlatformRun run, AgentAccount account,
                            CampaignCandidateTrace trace, String jobTitle) {
        PlatformAdapter adapter = adapterRegistry.get(run.getPlatform());
        int attempt = countRechatAttempts(trace) + 1;
        String message = greetingComposer.composeGreeting(campaign.getJobId(), jobTitle,
                trace.getCandidateName(), "RECHAT");
        adapter.sendFollowUp(account, trace.getPlatformUserId(), message);
        appendTimeline(trace, "RECHAT", "第" + attempt + "次复聊");
        trace.setUpdatedAt(LocalDateTime.now());
        traceMapper.updateById(trace);
        evolutionEmitter.emitRechat(campaign.getJobId(), campaign.getId(), trace.getId(),
                trace.getCandidateId(), attempt);
        logBehavior(run, account.getId(), "RECHAT", trace.getPlatform(),
                "复聊 " + trace.getCandidateName() + " 第" + attempt + "次", true, null);
    }

    private boolean shouldRechat(JobSourcingCampaign campaign, CampaignCandidateTrace trace) {
        Map<String, Object> policy = readRechatPolicy(campaign);
        int maxAttempts = policy.get("maxAttempts") instanceof Number
                ? ((Number) policy.get("maxAttempts")).intValue() : 2;
        if (countRechatAttempts(trace) >= maxAttempts) {
            return false;
        }
        long intervalMs = rechatIntervalMs(policy);
        LocalDateTime last = lastOutboundAt(trace);
        if (last == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(last.plus(intervalMs, java.time.temporal.ChronoUnit.MILLIS));
    }

    private long rechatIntervalMs(Map<String, Object> policy) {
        int hours = policy.get("intervalHours") instanceof Number
                ? ((Number) policy.get("intervalHours")).intValue() : 48;
        if (!rpaEnabled()) {
            return 2 * 60 * 1000L;
        }
        return hours * 3600_000L;
    }

    private int countRechatAttempts(CampaignCandidateTrace trace) {
        return countTimelineEvents(trace, "RECHAT");
    }

    private LocalDateTime lastOutboundAt(CampaignCandidateTrace trace) {
        try {
            if (!StringUtils.hasText(trace.getTimelineJson())) {
                return trace.getUpdatedAt();
            }
            List<Map<String, Object>> timeline = objectMapper.readValue(trace.getTimelineJson(),
                    new TypeReference<List<Map<String, Object>>>() {});
            LocalDateTime last = null;
            for (Map<String, Object> entry : timeline) {
                String event = entry.get("event") != null ? entry.get("event").toString() : "";
                if ("GREET_SENT".equals(event) || "RECHAT".equals(event)) {
                    Object at = entry.get("at");
                    if (at != null) {
                        LocalDateTime t = LocalDateTime.parse(at.toString());
                        if (last == null || t.isAfter(last)) {
                            last = t;
                        }
                    }
                }
            }
            return last != null ? last : trace.getUpdatedAt();
        } catch (Exception e) {
            return trace.getUpdatedAt();
        }
    }

    private int countTimelineEvents(CampaignCandidateTrace trace, String eventName) {
        try {
            if (!StringUtils.hasText(trace.getTimelineJson())) {
                return 0;
            }
            List<Map<String, Object>> timeline = objectMapper.readValue(trace.getTimelineJson(),
                    new TypeReference<List<Map<String, Object>>>() {});
            int count = 0;
            for (Map<String, Object> entry : timeline) {
                if (eventName.equals(String.valueOf(entry.get("event")))) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    private void maybeImport(JobSourcingCampaign campaign, CampaignCandidateTrace trace,
                             PlatformCandidate pc, PlatformResume resume) {
        boolean needConfirm = campaign.getResumeConfirmRequired() != null && campaign.getResumeConfirmRequired() == 1;
        if (needConfirm || "SEMI_AUTO".equals(campaign.getMode())) {
            trace.setTraceStatus("PENDING_IMPORT");
            traceMapper.updateById(trace);
            return;
        }
        doImport(campaign, trace, pc, resume);
    }

    private void doImport(JobSourcingCampaign campaign, CampaignCandidateTrace trace,
                          PlatformCandidate pc, PlatformResume resume) {
        BigDecimal score = trace.getMatchScore() != null ? trace.getMatchScore() : pc.getMatchScore();
        CandidateImportService.ImportResult result = importService.importCandidate(
                campaign.getTenantId(), campaign.getJobId(), pc, resume, score);
        trace.setCandidateId(result.getCandidateId());
        trace.setResumeId(result.getResumeId());
        trace.setTraceStatus("IMPORTED");
        trace.setUpdatedAt(LocalDateTime.now());
        traceMapper.updateById(trace);

        LambdaQueryWrapper<CandidateAcquireLock> lw = new LambdaQueryWrapper<>();
        lw.eq(CandidateAcquireLock::getCampaignId, campaign.getId())
                .eq(CandidateAcquireLock::getDedupKey, trace.getDedupKey());
        CandidateAcquireLock lock = lockMapper.selectOne(lw);
        if (lock != null) {
            lock.setCandidateId(result.getCandidateId());
            lock.setStatus("IMPORTED");
            lockMapper.updateById(lock);
        }
    }

    private void createSkippedTrace(JobSourcingCampaign campaign, CampaignPlatformRun run,
                                    AgentAccount account, PlatformCandidate pc, String dedupKey,
                                    String reason, String screenStage, String skipSummary) {
        CampaignCandidateTrace trace = newTrace(campaign, run, account, pc, dedupKey);
        trace.setTraceStatus("DUPLICATE_SKIPPED");
        trace.setSkipReason(reason);
        trace.setScreenStage(screenStage);
        if (StringUtils.hasText(skipSummary)) {
            trace.setSkipReason(skipSummary);
        }
        trace.setOpsPackVersion(readOpsPackVersion(campaign));
        traceMapper.insert(trace);
    }

    private void createScreenSkippedTrace(JobSourcingCampaign campaign, CampaignPlatformRun run,
                                          AgentAccount account, PlatformCandidate pc, String dedupKey,
                                          ScreeningEngine.ScreeningResult result) {
        CampaignCandidateTrace trace = newTrace(campaign, run, account, pc, dedupKey);
        trace.setTraceStatus("SCREEN_SKIPPED");
        trace.setScreenStage(result.getStage());
        trace.setSkipReason(result.getHumanReason());
        trace.setOpsPackVersion(readOpsPackVersion(campaign));
        try {
            if (result.getSkipReasonJson() != null) {
                trace.setSkipReasonJson(objectMapper.writeValueAsString(result.getSkipReasonJson()));
            }
        } catch (Exception ignored) {
        }
        appendTimeline(trace, "SCREEN_SKIP", result.getHumanReason());
        traceMapper.insert(trace);
    }

    private void saveStagingCandidate(JobSourcingCampaign campaign, CampaignCandidateTrace trace,
                                      PlatformCandidate pc) {
        ChannelStagingCandidate staging = new ChannelStagingCandidate();
        staging.setId(IdWorker.getId());
        staging.setTenantId(campaign.getTenantId());
        staging.setJobId(campaign.getJobId());
        staging.setCampaignId(campaign.getId());
        staging.setTraceId(trace.getId());
        staging.setPlatform(trace.getPlatform());
        staging.setPlatformUserId(pc.getPlatformUserId());
        staging.setCandidateName(pc.getName());
        staging.setMatchScore(pc.getMatchScore());
        staging.setStatus("STAGED");
        staging.setCreatedAt(LocalDateTime.now());
        staging.setUpdatedAt(LocalDateTime.now());
        stagingMapper.insert(staging);
    }

    private void emitScreenSignal(JobSourcingCampaign campaign, CampaignCandidateTrace trace,
                                  PlatformCandidate pc, ScreeningEngine.ScreeningResult result, boolean passed) {
        Map<String, Object> adj = new LinkedHashMap<>();
        if (result.getSkipReasonJson() != null && result.getSkipReasonJson().get("rule") != null) {
            adj.put(result.getSkipReasonJson().get("rule").toString(), passed ? 0.05 : -0.08);
        } else {
            adj.put("_screen", passed ? 0.05 : -0.05);
        }
        Long traceId = trace != null ? trace.getId() : null;
        evolutionEmitter.emitScreen(campaign.getJobId(), campaign.getId(), traceId, null, passed, adj);
    }

    private void appendTimeline(CampaignCandidateTrace trace, String event, String detail) {
        try {
            List<Map<String, Object>> timeline = new ArrayList<>();
            if (StringUtils.hasText(trace.getTimelineJson())) {
                timeline = objectMapper.readValue(trace.getTimelineJson(),
                        new TypeReference<List<Map<String, Object>>>() {});
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("event", event);
            entry.put("detail", detail);
            entry.put("at", LocalDateTime.now().toString());
            timeline.add(entry);
            trace.setTimelineJson(objectMapper.writeValueAsString(timeline));
        } catch (Exception ignored) {
        }
    }

    private void applyPlatformMatchScore(JobSourcingCampaign campaign, PlatformCandidate pc) {
        if (campaign.getJobId() == null || pc == null) {
            return;
        }
        Long tenantId = TenantContext.getTenantId();
        String jobTags = jobReadMapper.selectJobTags(campaign.getJobId(), tenantId);
        TagMatchScoreCalculator.CandidateSkillProfile profile = new TagMatchScoreCalculator.CandidateSkillProfile();
        StringBuilder skillBlob = new StringBuilder();
        if (StringUtils.hasText(pc.getTitle())) {
            skillBlob.append(pc.getTitle()).append(',');
        }
        if (StringUtils.hasText(pc.getCompany())) {
            skillBlob.append(pc.getCompany());
        }
        profile.setTags(skillBlob.length() > 0 ? skillBlob.toString() : null);
        profile.setCurrentTitle(pc.getTitle());
        profile.setCurrentCompany(pc.getCompany());
        profile.setWorkYears(pc.getWorkYears());
        profile.setHasParsedResume(false);
        pc.setMatchScore(TagMatchScoreCalculator.calculate(jobTags, profile, objectMapper));
    }

    private Object readScreeningProfile(JobSourcingCampaign campaign) {
        Map<String, Object> cfg = readCampaignConfig(campaign);
        return cfg != null ? cfg.get("screeningProfile") : null;
    }

    private String readGreetStrategy(JobSourcingCampaign campaign) {
        Map<String, Object> cfg = readCampaignConfig(campaign);
        if (cfg != null && cfg.get("greetStrategy") != null) {
            return cfg.get("greetStrategy").toString();
        }
        return "SCREEN_THEN_GREET";
    }

    private String readSearchSource(JobSourcingCampaign campaign) {
        Map<String, Object> cfg = readCampaignConfig(campaign);
        if (cfg != null && cfg.get("searchSource") != null) {
            return cfg.get("searchSource").toString();
        }
        return "RECOMMEND";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readPlatformQuotas(JobSourcingCampaign campaign) {
        Map<String, Object> cfg = readCampaignConfig(campaign);
        if (cfg != null && cfg.get("platformQuotas") instanceof Map) {
            return (Map<String, Object>) cfg.get("platformQuotas");
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readRechatPolicy(JobSourcingCampaign campaign) {
        Map<String, Object> cfg = readCampaignConfig(campaign);
        if (cfg != null && cfg.get("rechatPolicy") instanceof Map) {
            return (Map<String, Object>) cfg.get("rechatPolicy");
        }
        Map<String, Object> def = new LinkedHashMap<>();
        def.put("maxAttempts", 2);
        def.put("intervalHours", 48);
        return def;
    }

    private Integer readOpsPackVersion(JobSourcingCampaign campaign) {
        if (campaign.getOpsPackVersion() != null) {
            return campaign.getOpsPackVersion();
        }
        Map<String, Object> cfg = readCampaignConfig(campaign);
        if (cfg != null && cfg.get("opsPackVersion") instanceof Number) {
            return ((Number) cfg.get("opsPackVersion")).intValue();
        }
        return null;
    }

    private Map<String, Object> readCampaignConfig(JobSourcingCampaign campaign) {
        try {
            if (!StringUtils.hasText(campaign.getConfigJson())) {
                return null;
            }
            return objectMapper.readValue(campaign.getConfigJson(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private CampaignCandidateTrace newTrace(JobSourcingCampaign campaign, CampaignPlatformRun run,
                                            AgentAccount account, PlatformCandidate pc, String dedupKey) {
        CampaignCandidateTrace trace = new CampaignCandidateTrace();
        trace.setId(IdWorker.getId());
        trace.setTenantId(campaign.getTenantId());
        trace.setCampaignId(campaign.getId());
        trace.setPlatformRunId(run.getId());
        trace.setJobId(campaign.getJobId());
        trace.setPlatform(run.getPlatform());
        trace.setAccountId(account.getId());
        trace.setPlatformUserId(pc.getPlatformUserId());
        trace.setCandidateName(pc.getName());
        trace.setPhone(pc.getPhone());
        trace.setEmail(pc.getEmail());
        trace.setDedupKey(dedupKey);
        trace.setMatchScore(pc.getMatchScore());
        trace.setCreatedAt(LocalDateTime.now());
        trace.setUpdatedAt(LocalDateTime.now());
        return trace;
    }

    private PlatformCandidate toPlatformCandidate(CampaignCandidateTrace trace) {
        PlatformCandidate pc = new PlatformCandidate();
        pc.setPlatformUserId(trace.getPlatformUserId());
        pc.setName(trace.getCandidateName());
        pc.setPhone(trace.getPhone());
        pc.setEmail(trace.getEmail());
        pc.setMatchScore(trace.getMatchScore());
        return pc;
    }

    private String buildDedupKey(String platform, PlatformCandidate pc) {
        if (StringUtils.hasText(pc.getPhone())) {
            return platform + ":phone:" + pc.getPhone();
        }
        if (StringUtils.hasText(pc.getEmail())) {
            return platform + ":email:" + pc.getEmail();
        }
        return platform + ":uid:" + pc.getPlatformUserId();
    }

    private void logBehavior(CampaignPlatformRun run, Long accountId, String actionType, String platform,
                             String detail, boolean success, String error) {
        AgentBehaviorLog log = new AgentBehaviorLog();
        log.setId(IdWorker.getId());
        log.setTenantId(run.getTenantId());
        log.setAgentAccountId(accountId);
        log.setAction(actionType);
        log.setActionType(actionType);
        log.setTargetPlatform(platform);
        log.setActionDetail(detail);
        log.setIsSuccess(success ? 1 : 0);
        log.setSuccess(success ? 1 : 0);
        log.setErrorMessage(error);
        log.setExecutedAt(LocalDateTime.now());
        log.setCreatedAt(LocalDateTime.now());
        behaviorLogMapper.insert(log);
    }

    private List<String> readKeywords(JobSourcingCampaign campaign) {
        try {
            if (!StringUtils.hasText(campaign.getConfigJson())) {
                return Collections.singletonList("工程师");
            }
            Map<String, Object> cfg = objectMapper.readValue(campaign.getConfigJson(), new TypeReference<Map<String, Object>>() {});
            Object kw = cfg.get("keywords");
            if (kw instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) kw;
                if (!list.isEmpty()) {
                    return list;
                }
            }
        } catch (Exception ignored) {
        }
        return Collections.singletonList("工程师");
    }

    private boolean readPublishPending(JobSourcingCampaign campaign) {
        return campaign.getPublishConfirmRequired() != null && campaign.getPublishConfirmRequired() == 1;
    }

    private boolean readRunFlag(CampaignPlatformRun run, String key) {
        try {
            if (!StringUtils.hasText(run.getStatsJson())) {
                return false;
            }
            Map<String, Object> m = objectMapper.readValue(run.getStatsJson(), new TypeReference<Map<String, Object>>() {});
            return Boolean.TRUE.equals(m.get(key));
        } catch (Exception e) {
            return false;
        }
    }

    private void writeRunFlag(CampaignPlatformRun run, String key, boolean value) {
        try {
            Map<String, Object> m = new HashMap<>();
            if (StringUtils.hasText(run.getStatsJson())) {
                m = objectMapper.readValue(run.getStatsJson(), new TypeReference<Map<String, Object>>() {});
            }
            m.put(key, value);
            run.setStatsJson(objectMapper.writeValueAsString(m));
        } catch (Exception ignored) {
        }
    }

    private boolean isPlatformRunIdle(Long campaignId, Long platformRunId) {
        LambdaQueryWrapper<CampaignCandidateTrace> w = new LambdaQueryWrapper<>();
        w.eq(CampaignCandidateTrace::getCampaignId, campaignId)
                .eq(CampaignCandidateTrace::getPlatformRunId, platformRunId)
                .notIn(CampaignCandidateTrace::getTraceStatus,
                        Arrays.asList("IMPORTED", "DUPLICATE_SKIPPED", "SCREEN_SKIPPED", "STAGED"));
        return traceMapper.selectCount(w) == 0 && readRunFlag(platformRunMapper.selectById(platformRunId), "searchDone");
    }

    private List<Long> parseAuxIds(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public void refreshCampaignStats(JobSourcingCampaign campaign) {
        LambdaQueryWrapper<CampaignCandidateTrace> w = new LambdaQueryWrapper<>();
        w.eq(CampaignCandidateTrace::getCampaignId, campaign.getId());
        List<CampaignCandidateTrace> traces = traceMapper.selectList(w);
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("searched", 0);
        stats.put("locked", 0);
        stats.put("greeted", 0);
        stats.put("replied", 0);
        stats.put("resumes", 0);
        stats.put("imported", 0);
        stats.put("pendingScreening", 0);
        stats.put("duplicatesSkipped", 0);
        stats.put("screenSkipped", 0);
        stats.put("alerts", 0);

        LambdaQueryWrapper<CampaignPlatformRun> pr = new LambdaQueryWrapper<>();
        pr.eq(CampaignPlatformRun::getCampaignId, campaign.getId());
        int published = 0;
        for (CampaignPlatformRun run : platformRunMapper.selectList(pr)) {
            if (StringUtils.hasText(run.getPlatformJobUrl())) {
                published++;
            }
        }
        stats.put("published", published);

        for (CampaignCandidateTrace t : traces) {
            String st = t.getTraceStatus();
            if ("DUPLICATE_SKIPPED".equals(st)) {
                stats.put("duplicatesSkipped", stats.get("duplicatesSkipped") + 1);
            }
            if ("SCREEN_SKIPPED".equals(st)) {
                stats.put("screenSkipped", stats.get("screenSkipped") + 1);
            }
            if (!Arrays.asList("DUPLICATE_SKIPPED", "SCREEN_SKIPPED").contains(st)) {
                stats.put("searched", stats.get("searched") + 1);
            }
            if (Arrays.asList("LOCKED", "PENDING_GREET_CONFIRM", "GREETED", "MONITORING", "RESUME_READY", "PENDING_IMPORT", "IMPORTED").contains(st)) {
                stats.put("locked", stats.get("locked") + 1);
            }
            if (Arrays.asList("GREETED", "MONITORING", "RESUME_READY", "PENDING_IMPORT", "IMPORTED").contains(st)) {
                stats.put("greeted", stats.get("greeted") + 1);
            }
            if (Arrays.asList("MONITORING", "RESUME_READY", "PENDING_IMPORT", "IMPORTED").contains(st)) {
                stats.put("replied", stats.get("replied") + 1);
            }
            if (Arrays.asList("RESUME_READY", "PENDING_IMPORT", "IMPORTED").contains(st)) {
                stats.put("resumes", stats.get("resumes") + 1);
            }
            if ("IMPORTED".equals(st)) {
                stats.put("imported", stats.get("imported") + 1);
                stats.put("pendingScreening", stats.get("pendingScreening") + 1);
            }
        }
        try {
            campaign.setStatsJson(objectMapper.writeValueAsString(stats));
            campaignMapper.updateById(campaign);
        } catch (Exception e) {
            log.warn("stats serialize failed", e);
        }
    }

    private void checkCampaignComplete(JobSourcingCampaign campaign) {
        LambdaQueryWrapper<CampaignPlatformRun> w = new LambdaQueryWrapper<>();
        w.eq(CampaignPlatformRun::getCampaignId, campaign.getId());
        List<CampaignPlatformRun> runs = platformRunMapper.selectList(w);
        boolean allDone = !runs.isEmpty();
        for (CampaignPlatformRun run : runs) {
            if (!"COMPLETED".equals(run.getStatus()) && !"FAILED".equals(run.getStatus())) {
                allDone = false;
                break;
            }
        }
        if (allDone) {
            campaign.setStatus("COMPLETED");
            campaign.setCompletedAt(LocalDateTime.now());
            campaignMapper.updateById(campaign);
        }
    }

    private JobSourcingCampaign requireCampaign(Long id) {
        JobSourcingCampaign c = campaignMapper.selectById(id);
        if (c == null) {
            throw new com.recruitos.common.exception.BizException("Campaign not found");
        }
        return c;
    }

    private boolean rpaEnabled() {
        return rpaProperties != null && rpaProperties.isEnabled();
    }

    private String normalizeStep(String step) {
        if (!StringUtils.hasText(step)) {
            return STEP_LOGIN;
        }
        String s = step.trim().toUpperCase();
        if (STEP_MONITOR.equals(s) || STEP_GREET.equals(s) || STEP_FETCH.equals(s)) {
            return STEP_ACTIVE;
        }
        return s;
    }

    private boolean hasRunnableRuns(Long campaignId) {
        LambdaQueryWrapper<CampaignPlatformRun> w = new LambdaQueryWrapper<>();
        w.eq(CampaignPlatformRun::getCampaignId, campaignId)
                .eq(CampaignPlatformRun::getStatus, "RUNNING");
        for (CampaignPlatformRun run : platformRunMapper.selectList(w)) {
            AgentAccount primary = accountMapper.selectById(run.getPrimaryAccountId());
            if (automationGuard.isAccountRunnable(primary)) {
                return true;
            }
        }
        return false;
    }
}
