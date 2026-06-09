package com.recruitos.evolution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitos.common.auth.CurrentUser;
import com.recruitos.common.evolution.EvolutionSignalLevel;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.evolution.dto.ProposalQueryDTO;
import com.recruitos.evolution.dto.ProposalVO;
import com.recruitos.evolution.entity.EvolutionProposal;
import com.recruitos.evolution.entity.EvolutionSignal;
import com.recruitos.evolution.entity.JobWeightSnapshot;
import com.recruitos.evolution.mapper.EvolutionProposalMapper;
import com.recruitos.evolution.mapper.EvolutionSignalMapper;
import com.recruitos.evolution.mapper.JobOpsPackReadMapper;
import com.recruitos.evolution.mapper.JobWeightSnapshotMapper;
import com.recruitos.evolution.dto.HealthVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class EvolutionProposalService {

    private static final Logger log = LoggerFactory.getLogger(EvolutionProposalService.class);
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPLIED = "APPLIED";
    private static final String STATUS_REJECTED = "REJECTED";

    @Resource
    private EvolutionProposalMapper proposalMapper;
    @Resource
    private EvolutionSignalMapper signalMapper;
    @Resource
    private JobOpsPackReadMapper opsPackReadMapper;
    @Resource
    private JobWeightSnapshotMapper jobWeightSnapshotMapper;
    @Resource
    private HealthService healthService;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private EvolutionSettingsService evolutionSettingsService;

    @Value("${recruitos.job.base-url:http://localhost:8084}")
    private String jobBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public PageResult<ProposalVO> list(ProposalQueryDTO query) {
        Long tenantId = TenantContext.getTenantId();
        Page<EvolutionProposal> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<EvolutionProposal> w = new LambdaQueryWrapper<>();
        w.eq(EvolutionProposal::getTenantId, tenantId);
        if (query.getJobId() != null) {
            w.eq(EvolutionProposal::getJobId, query.getJobId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            w.eq(EvolutionProposal::getStatus, query.getStatus());
        } else {
            w.eq(EvolutionProposal::getStatus, STATUS_PENDING);
        }
        w.orderByDesc(EvolutionProposal::getCreatedAt);
        Page<EvolutionProposal> result = proposalMapper.selectPage(page, w);
        List<ProposalVO> list = new ArrayList<>();
        for (EvolutionProposal p : result.getRecords()) {
            list.add(toVO(p));
        }
        return new PageResult<>(result.getTotal(), list, query.getPageNum(), query.getPageSize());
    }

    public ProposalVO getById(Long id) {
        EvolutionProposal p = requireProposal(id);
        return toVO(p);
    }

    @Transactional
    public ProposalVO confirm(Long id) {
        EvolutionProposal proposal = requireProposal(id);
        if (!STATUS_PENDING.equals(proposal.getStatus())) {
            throw new BizException("仅待确认建议可采纳");
        }
        Map<String, Object> pack = parseJsonMap(proposal.getProposedOpsPackJson());
        if (pack == null || pack.isEmpty()) {
            throw new BizException("建议内容为空");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("pack", pack);
        body.put("proposalId", proposal.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            headers.set("X-Tenant-Id", tenantId.toString());
        }
        Long userId = CurrentUser.getCurrentUserId();
        if (userId != null) {
            headers.set("X-User-Id", userId.toString());
        }

        try {
            restTemplate.postForEntity(
                    jobBaseUrl + "/api/job/" + proposal.getJobId() + "/ops-pack/apply-evolution",
                    new HttpEntity<>(body, headers),
                    Map.class);
        } catch (Exception e) {
            log.warn("Apply evolution ops pack failed: {}", e.getMessage());
            throw new BizException("应用运营包失败: " + e.getMessage());
        }

        proposal.setStatus(STATUS_APPLIED);
        proposal.setReviewedBy(userId);
        proposal.setReviewedAt(LocalDateTime.now());
        proposalMapper.updateById(proposal);
        return toVO(proposal);
    }

    @Transactional
    public ProposalVO reject(Long id, String reason) {
        EvolutionProposal proposal = requireProposal(id);
        if (!STATUS_PENDING.equals(proposal.getStatus())) {
            throw new BizException("仅待确认建议可驳回");
        }
        proposal.setStatus(STATUS_REJECTED);
        proposal.setRejectReason(reason);
        proposal.setReviewedBy(CurrentUser.getCurrentUserId());
        proposal.setReviewedAt(LocalDateTime.now());
        proposalMapper.updateById(proposal);
        return toVO(proposal);
    }

    @Scheduled(fixedDelay = 60000)
    public void generateProposalsBatch() {
        LambdaQueryWrapper<EvolutionSignal> w = new LambdaQueryWrapper<>();
        w.eq(EvolutionSignal::getStatus, STATUS_PENDING)
                .eq(EvolutionSignal::getSignalLevel, EvolutionSignalLevel.L3_SCREEN);
        List<EvolutionSignal> signals = signalMapper.selectList(w);
        Map<String, List<EvolutionSignal>> byJob = new LinkedHashMap<>();
        for (EvolutionSignal s : signals) {
            String key = s.getTenantId() + ":" + s.getJobId();
            byJob.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
        }
        for (List<EvolutionSignal> group : byJob.values()) {
            Long tenantId = group.isEmpty() ? null : group.get(0).getTenantId();
            int minSignals = tenantId != null
                    ? evolutionSettingsService.resolveMinSignals(tenantId)
                    : 15;
            if (group.size() < minSignals) {
                continue;
            }
            EvolutionSignal sample = group.get(0);
            try {
                TenantContext.setTenantId(sample.getTenantId());
                if (hasPendingProposal(sample.getJobId())) {
                    continue;
                }
                createProposalFromSignals(sample.getTenantId(), sample.getJobId(), group);
            } catch (Exception e) {
                log.warn("Proposal generation failed jobId={}: {}", sample.getJobId(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    /** G4：健康 CRITICAL 时生成回滚建议，不自动回滚 */
    @Scheduled(fixedDelay = 120000)
    public void generateRollbackProposalsFromHealth() {
        LambdaQueryWrapper<JobWeightSnapshot> w = new LambdaQueryWrapper<>();
        w.select(JobWeightSnapshot::getTenantId, JobWeightSnapshot::getJobId)
                .groupBy(JobWeightSnapshot::getTenantId, JobWeightSnapshot::getJobId);
        List<JobWeightSnapshot> jobs = jobWeightSnapshotMapper.selectList(w);
        for (JobWeightSnapshot row : jobs) {
            try {
                TenantContext.setTenantId(row.getTenantId());
                if (hasPendingProposalOfType(row.getJobId(), "ROLLBACK")) {
                    continue;
                }
                HealthVO health = healthService.getJobHealth(row.getJobId());
                if (!"CRITICAL".equals(health.getStatus())) {
                    continue;
                }
                createRollbackProposal(row.getTenantId(), row.getJobId(), health);
            } catch (Exception e) {
                log.warn("Rollback proposal failed jobId={}: {}", row.getJobId(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    @Scheduled(fixedDelay = 3600000)
    public void expireStaleProposals() {
        LambdaQueryWrapper<EvolutionProposal> w = new LambdaQueryWrapper<>();
        w.eq(EvolutionProposal::getStatus, STATUS_PENDING)
                .lt(EvolutionProposal::getExpiresAt, LocalDateTime.now());
        for (EvolutionProposal p : proposalMapper.selectList(w)) {
            p.setStatus("EXPIRED");
            proposalMapper.updateById(p);
        }
    }

    private void createRollbackProposal(Long tenantId, Long jobId, HealthVO health) throws Exception {
        Map<String, Object> activeRow = opsPackReadMapper.selectActivePack(tenantId, jobId);
        Map<String, Object> archivedRow = opsPackReadMapper.selectLatestArchivedPack(tenantId, jobId);
        if (archivedRow == null || archivedRow.get("packJson") == null) {
            return;
        }
        int activeVersion = activeRow != null && activeRow.get("version") instanceof Number
                ? ((Number) activeRow.get("version")).intValue() : 1;
        int rollbackVersion = archivedRow.get("version") instanceof Number
                ? ((Number) archivedRow.get("version")).intValue() : activeVersion - 1;

        Map<String, Object> rollbackPack = objectMapper.readValue(
                archivedRow.get("packJson").toString(), new TypeReference<Map<String, Object>>() {});

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("healthStatus", health.getStatus());
        evidence.put("overallScore", health.getOverallScore());
        evidence.put("alerts", health.getAlerts());

        Map<String, Object> diff = new LinkedHashMap<>();
        diff.put("opsPackVersion", activeVersion + " → " + rollbackVersion);

        EvolutionProposal proposal = new EvolutionProposal();
        proposal.setTenantId(tenantId);
        proposal.setJobId(jobId);
        proposal.setProposalType("ROLLBACK");
        proposal.setStatus(STATUS_PENDING);
        proposal.setTitle(String.format("健康告警：建议回滚运营包 v%d → v%d", activeVersion, rollbackVersion));
        proposal.setDiffJson(objectMapper.writeValueAsString(diff));
        proposal.setEvidenceJson(objectMapper.writeValueAsString(evidence));
        proposal.setProposedOpsPackJson(objectMapper.writeValueAsString(rollbackPack));
        proposal.setBaseOpsPackVersion(activeVersion);
        proposal.setExpiresAt(LocalDateTime.now().plusDays(7));
        proposalMapper.insert(proposal);
    }

    private boolean hasPendingProposalOfType(Long jobId, String proposalType) {
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<EvolutionProposal> w = new LambdaQueryWrapper<>();
        w.eq(EvolutionProposal::getTenantId, tenantId)
                .eq(EvolutionProposal::getJobId, jobId)
                .eq(EvolutionProposal::getStatus, STATUS_PENDING)
                .eq(EvolutionProposal::getProposalType, proposalType);
        return proposalMapper.selectCount(w) > 0;
    }

    private void createProposalFromSignals(Long tenantId, Long jobId, List<EvolutionSignal> signals) throws Exception {
        Map<String, Double> tagDeltas = new LinkedHashMap<>();
        int skipCount = 0;
        int passCount = 0;
        for (EvolutionSignal s : signals) {
            if ("SCREEN_SKIP".equals(s.getSourceEvent())) {
                skipCount++;
            } else if ("SCREEN_PASS".equals(s.getSourceEvent())) {
                passCount++;
            }
            mergeAdjustments(tagDeltas, s);
        }

        Map<String, Object> activePackRow = opsPackReadMapper.selectActivePack(tenantId, jobId);
        Map<String, Object> basePack = new LinkedHashMap<>();
        int baseVersion = 1;
        if (activePackRow != null) {
            if (activePackRow.get("version") instanceof Number) {
                baseVersion = ((Number) activePackRow.get("version")).intValue();
            }
            Object packJson = activePackRow.get("packJson");
            if (packJson != null) {
                basePack = objectMapper.readValue(packJson.toString(), new TypeReference<Map<String, Object>>() {});
            }
        }

        Map<String, Object> proposed = deepCopy(basePack);
        @SuppressWarnings("unchecked")
        Map<String, Object> screening = proposed.containsKey("screeningProfile")
                ? (Map<String, Object>) proposed.get("screeningProfile") : new LinkedHashMap<>();
        double threshold = screening.get("passThreshold") instanceof Number
                ? ((Number) screening.get("passThreshold")).doubleValue() : 60.0;
        if (skipCount > passCount) {
            threshold = Math.min(threshold + 3, 85);
        } else {
            threshold = Math.max(threshold - 2, 45);
        }
        screening.put("passThreshold", (int) threshold);
        proposed.put("screeningProfile", screening);

        Map<String, Object> diff = new LinkedHashMap<>();
        Map<String, Object> thresholdChange = new LinkedHashMap<>();
        Object oldThreshold = 60;
        if (basePack.get("screeningProfile") instanceof Map) {
            Object pt = ((Map<?, ?>) basePack.get("screeningProfile")).get("passThreshold");
            if (pt != null) {
                oldThreshold = pt;
            }
        }
        thresholdChange.put("from", oldThreshold);
        thresholdChange.put("to", (int) threshold);
        diff.put("passThreshold", thresholdChange);

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("signalCount", signals.size());
        evidence.put("skipCount", skipCount);
        evidence.put("passCount", passCount);
        evidence.put("tagDeltas", tagDeltas);

        EvolutionProposal proposal = new EvolutionProposal();
        proposal.setTenantId(tenantId);
        proposal.setJobId(jobId);
        proposal.setProposalType("OPS_PACK");
        proposal.setStatus(STATUS_PENDING);
        int oldThresholdInt = oldThreshold instanceof Number
                ? ((Number) oldThreshold).intValue() : 60;
        proposal.setTitle(String.format("筛选策略调整：及格线 %d → %d（基于 %d 条信号）",
                oldThresholdInt, (int) threshold, signals.size()));
        proposal.setDiffJson(objectMapper.writeValueAsString(diff));
        proposal.setEvidenceJson(objectMapper.writeValueAsString(evidence));
        proposal.setProposedOpsPackJson(objectMapper.writeValueAsString(proposed));
        proposal.setBaseOpsPackVersion(baseVersion);
        proposal.setExpiresAt(LocalDateTime.now().plusDays(7));
        proposalMapper.insert(proposal);
    }

    private boolean hasPendingProposal(Long jobId) {
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<EvolutionProposal> w = new LambdaQueryWrapper<>();
        w.eq(EvolutionProposal::getTenantId, tenantId)
                .eq(EvolutionProposal::getJobId, jobId)
                .eq(EvolutionProposal::getStatus, STATUS_PENDING);
        return proposalMapper.selectCount(w) > 0;
    }

    private void mergeAdjustments(Map<String, Double> target, EvolutionSignal signal) {
        if (!StringUtils.hasText(signal.getTagAdjustments())) {
            return;
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(signal.getTagAdjustments(),
                    new TypeReference<Map<String, Object>>() {});
            for (Map.Entry<String, Object> e : raw.entrySet()) {
                double v = e.getValue() instanceof Number ? ((Number) e.getValue()).doubleValue() : 0;
                target.merge(e.getKey(), v, Double::sum);
            }
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCopy(Map<String, Object> src) throws Exception {
        if (src == null || src.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return objectMapper.readValue(objectMapper.writeValueAsString(src),
                new TypeReference<Map<String, Object>>() {});
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private EvolutionProposal requireProposal(Long id) {
        Long tenantId = TenantContext.getTenantId();
        EvolutionProposal p = proposalMapper.selectById(id);
        if (p == null || !p.getTenantId().equals(tenantId)) {
            throw new BizException("进化建议不存在");
        }
        return p;
    }

    private ProposalVO toVO(EvolutionProposal p) {
        ProposalVO vo = new ProposalVO();
        vo.setId(p.getId());
        vo.setJobId(p.getJobId());
        vo.setProposalType(p.getProposalType());
        vo.setStatus(p.getStatus());
        vo.setTitle(p.getTitle());
        vo.setDiff(parseJsonMap(p.getDiffJson()));
        vo.setEvidence(parseJsonMap(p.getEvidenceJson()));
        vo.setProposedOpsPack(parseJsonMap(p.getProposedOpsPackJson()));
        vo.setBaseOpsPackVersion(p.getBaseOpsPackVersion());
        vo.setRejectReason(p.getRejectReason());
        vo.setExpiresAt(p.getExpiresAt());
        vo.setCreatedAt(p.getCreatedAt());
        vo.setReviewedAt(p.getReviewedAt());
        return vo;
    }
}
