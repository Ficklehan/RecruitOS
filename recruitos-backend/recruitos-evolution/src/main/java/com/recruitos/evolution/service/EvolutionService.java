package com.recruitos.evolution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitos.common.evolution.EvolutionSignalLevel;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.evolution.dto.*;
import com.recruitos.evolution.entity.EvolutionSignal;
import com.recruitos.evolution.entity.JobWeightSnapshot;
import com.recruitos.evolution.mapper.EvolutionSignalMapper;
import com.recruitos.evolution.mapper.JobWeightSnapshotMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EvolutionService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPLIED = "APPLIED";

    @Resource
    private EvolutionSignalMapper evolutionSignalMapper;

    @Resource
    private JobWeightSnapshotMapper jobWeightSnapshotMapper;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private CovarianceEvolutionService covarianceEvolutionService;

    @Transactional
    public SignalVO emitSignal(SignalEmitDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BizException("Tenant context required for signal emit");
        }

        EvolutionSignal signal = new EvolutionSignal();
        signal.setTenantId(tenantId);
        signal.setJobId(dto.getJobId());
        signal.setSignalLevel(dto.getSignalLevel());
        double conf = dto.getConfidence() != null
                ? dto.getConfidence()
                : EvolutionSignalLevel.defaultConfidence(dto.getSignalLevel());
        signal.setConfidence(BigDecimal.valueOf(conf).setScale(2, RoundingMode.HALF_UP));
        signal.setCandidateId(dto.getCandidateId());
        signal.setSourceModule(dto.getSourceModule());
        signal.setSourceEvent(dto.getSourceEvent());
        signal.setCampaignId(dto.getCampaignId());
        signal.setTraceId(dto.getTraceId());
        signal.setAbGroup(dto.getAbGroup());
        signal.setStatus(STATUS_PENDING);
        signal.setLearningRate(resolveLearningRate(dto.getJobId(), tenantId));

        if (dto.getTagAdjustments() != null && !dto.getTagAdjustments().isEmpty()) {
            try {
                signal.setTagAdjustments(objectMapper.writeValueAsString(dto.getTagAdjustments()));
            } catch (Exception e) {
                throw new BizException("Invalid tagAdjustments JSON");
            }
        }

        evolutionSignalMapper.insert(signal);
        return toSignalVO(signal);
    }

    /**
     * @deprecated 兼容旧接口，内部转 emit
     */
    @Transactional
    @Deprecated
    public SignalVO submitSignal(SignalSubmitDTO dto) {
        SignalEmitDTO emit = new SignalEmitDTO();
        emit.setJobId(dto.getJobId());
        emit.setSignalLevel(mapLegacySignalType(dto.getSignalType()));
        emit.setConfidence(dto.getSignalValue());
        emit.setSourceModule("legacy");
        emit.setSourceEvent(dto.getSource());
        Map<String, Object> adj = new LinkedHashMap<>();
        if (StringUtils.hasText(dto.getTagName())) {
            adj.put(dto.getTagName(), dto.getSignalValue());
        }
        emit.setTagAdjustments(adj);
        return emitSignal(emit);
    }

    public PageResult<SignalVO> getSignalList(SignalQueryDTO query) {
        Long tenantId = TenantContext.getTenantId();
        Page<EvolutionSignal> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<EvolutionSignal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EvolutionSignal::getTenantId, tenantId);
        if (query.getSignalLevel() != null) {
            wrapper.eq(EvolutionSignal::getSignalLevel, query.getSignalLevel());
        }
        if (query.getJobId() != null) {
            wrapper.eq(EvolutionSignal::getJobId, query.getJobId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(EvolutionSignal::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(EvolutionSignal::getCreatedAt);

        Page<EvolutionSignal> result = evolutionSignalMapper.selectPage(page, wrapper);
        List<SignalVO> voList = new ArrayList<>();
        for (EvolutionSignal signal : result.getRecords()) {
            voList.add(toSignalVO(signal));
        }
        return new PageResult<>(result.getTotal(), voList, query.getPageNum(), query.getPageSize());
    }

    public List<WeightSnapshotVO> getWeightSnapshot(Long jobId) {
        JobWeightSnapshot latest = latestSnapshot(jobId);
        List<WeightSnapshotVO> list = new ArrayList<>();
        if (latest != null) {
            list.add(toWeightSnapshotVO(latest));
        }
        return list;
    }

    public List<WeightSnapshotVO> getWeightHistory(Long jobId) {
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<JobWeightSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobWeightSnapshot::getTenantId, tenantId)
                .eq(JobWeightSnapshot::getJobId, jobId)
                .orderByDesc(JobWeightSnapshot::getCreatedAt);
        List<WeightSnapshotVO> list = new ArrayList<>();
        for (JobWeightSnapshot s : jobWeightSnapshotMapper.selectList(wrapper)) {
            list.add(toWeightSnapshotVO(s));
        }
        return list;
    }

    @Transactional
    public List<WeightSnapshotVO> triggerEvolution(Long jobId) {
        Long tenantId = TenantContext.getTenantId();

        LambdaQueryWrapper<EvolutionSignal> signalWrapper = new LambdaQueryWrapper<>();
        signalWrapper.eq(EvolutionSignal::getTenantId, tenantId)
                .eq(EvolutionSignal::getJobId, jobId)
                .eq(EvolutionSignal::getStatus, STATUS_PENDING);
        List<EvolutionSignal> signals = evolutionSignalMapper.selectList(signalWrapper);
        if (signals.isEmpty()) {
            throw new BizException("No pending signals found for job " + jobId);
        }

        Map<String, Double> mergedAdjustments = new LinkedHashMap<>();
        for (EvolutionSignal signal : signals) {
            mergeTagAdjustments(mergedAdjustments, signal);
        }

        Map<String, Double> shrunk = covarianceEvolutionService.applyShrinkage(jobId, mergedAdjustments);
        covarianceEvolutionService.recordSignalBatch(jobId, signals, mergedAdjustments);

        JobWeightSnapshot base = latestSnapshot(jobId);
        String newTagsJson = applyAdjustments(base, shrunk, signals.size());

        JobWeightSnapshot snapshot = new JobWeightSnapshot();
        snapshot.setTenantId(tenantId);
        snapshot.setJobId(jobId);
        snapshot.setSnapshotType("EVOLUTION");
        snapshot.setTagsSnapshot(newTagsJson);
        snapshot.setHealthScore(null);
        snapshot.setSignalId(signals.get(signals.size() - 1).getId());
        jobWeightSnapshotMapper.insert(snapshot);

        LocalDateTime now = LocalDateTime.now();
        for (EvolutionSignal signal : signals) {
            signal.setStatus(STATUS_APPLIED);
            signal.setAppliedAt(now);
            evolutionSignalMapper.updateById(signal);
        }

        List<WeightSnapshotVO> result = new ArrayList<>();
        result.add(toWeightSnapshotVO(snapshot));
        return result;
    }

    private JobWeightSnapshot latestSnapshot(Long jobId) {
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<JobWeightSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobWeightSnapshot::getTenantId, tenantId)
                .eq(JobWeightSnapshot::getJobId, jobId)
                .orderByDesc(JobWeightSnapshot::getCreatedAt)
                .last("LIMIT 1");
        return jobWeightSnapshotMapper.selectOne(wrapper);
    }

    private BigDecimal resolveLearningRate(Long jobId, Long tenantId) {
        LambdaQueryWrapper<EvolutionSignal> w = new LambdaQueryWrapper<>();
        w.eq(EvolutionSignal::getTenantId, tenantId).eq(EvolutionSignal::getJobId, jobId);
        long count = evolutionSignalMapper.selectCount(w);
        double alpha;
        if (count <= 20) {
            alpha = 0.15;
        } else if (count <= 50) {
            alpha = 0.08;
        } else {
            alpha = 0.02;
        }
        return BigDecimal.valueOf(alpha);
    }

    private void mergeTagAdjustments(Map<String, Double> target, EvolutionSignal signal) {
        if (!StringUtils.hasText(signal.getTagAdjustments())) {
            return;
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(
                    signal.getTagAdjustments(), new TypeReference<Map<String, Object>>() {});
            double weight = signal.getConfidence() != null ? signal.getConfidence().doubleValue() : 0.5;
            for (Map.Entry<String, Object> e : raw.entrySet()) {
                double delta = toDouble(e.getValue()) * weight;
                target.merge(e.getKey(), delta, Double::sum);
            }
        } catch (Exception ignored) {
            // skip malformed
        }
    }

    private String applyAdjustments(JobWeightSnapshot base, Map<String, Double> adjustments, int signalCount) {
        try {
            List<Map<String, Object>> tags = new ArrayList<>();
            if (base != null && StringUtils.hasText(base.getTagsSnapshot())) {
                tags = objectMapper.readValue(base.getTagsSnapshot(),
                        new TypeReference<List<Map<String, Object>>>() {});
            }
            Map<String, Map<String, Object>> byTag = new LinkedHashMap<>();
            for (Map<String, Object> tag : tags) {
                Object name = tag.get("tag");
                if (name != null) {
                    byTag.put(name.toString(), tag);
                }
            }
            for (Map.Entry<String, Double> adj : adjustments.entrySet()) {
                Map<String, Object> tag = byTag.computeIfAbsent(adj.getKey(), k -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("tag", k);
                    m.put("match_weight", 0.3);
                    m.put("search_weight", 0.3);
                    m.put("decision_weight", 0.3);
                    return m;
                });
                double delta = clamp(adj.getValue() * 0.1, -0.12, 0.12);
                tag.put("match_weight", clamp(toDouble(tag.get("match_weight")) + delta, 0.01, 1.0));
                tag.put("search_weight", clamp(toDouble(tag.get("search_weight")) + delta * 0.8, 0.01, 1.0));
                tag.put("decision_weight", clamp(toDouble(tag.get("decision_weight")) + delta * 0.6, 0.01, 1.0));
            }
            if (byTag.isEmpty() && adjustments.isEmpty()) {
                throw new BizException("No tag data to evolve");
            }
            return objectMapper.writeValueAsString(new ArrayList<>(byTag.values()));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("Failed to apply evolution adjustments");
        }
    }

    private int mapLegacySignalType(String signalType) {
        if (!StringUtils.hasText(signalType)) {
            return EvolutionSignalLevel.L3_SCREEN;
        }
        switch (signalType.toUpperCase()) {
            case "MATCH":
                return EvolutionSignalLevel.L3_SCREEN;
            case "SEARCH":
                return EvolutionSignalLevel.L5_GREET_RESUME;
            case "DECISION":
                return EvolutionSignalLevel.L2_INTERVIEW;
            default:
                return EvolutionSignalLevel.L3_SCREEN;
        }
    }

    private double toDouble(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        try {
            return Double.parseDouble(v.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private SignalVO toSignalVO(EvolutionSignal signal) {
        SignalVO vo = new SignalVO();
        vo.setId(signal.getId());
        vo.setTenantId(signal.getTenantId());
        vo.setJobId(signal.getJobId());
        vo.setSignalLevel(signal.getSignalLevel());
        vo.setConfidence(signal.getConfidence());
        vo.setCandidateId(signal.getCandidateId());
        vo.setSourceModule(signal.getSourceModule());
        vo.setSourceEvent(signal.getSourceEvent());
        vo.setCampaignId(signal.getCampaignId());
        vo.setTraceId(signal.getTraceId());
        vo.setTagAdjustments(signal.getTagAdjustments());
        vo.setStatus(signal.getStatus());
        vo.setAbGroup(signal.getAbGroup());
        vo.setCreatedAt(signal.getCreatedAt());
        vo.setUpdatedAt(signal.getUpdatedAt());
        return vo;
    }

    private WeightSnapshotVO toWeightSnapshotVO(JobWeightSnapshot snapshot) {
        WeightSnapshotVO vo = new WeightSnapshotVO();
        vo.setId(snapshot.getId());
        vo.setTenantId(snapshot.getTenantId());
        vo.setJobId(snapshot.getJobId());
        vo.setSnapshotType(snapshot.getSnapshotType());
        vo.setTagsSnapshot(snapshot.getTagsSnapshot());
        vo.setHealthScore(snapshot.getHealthScore());
        vo.setSignalId(snapshot.getSignalId());
        vo.setCreatedAt(snapshot.getCreatedAt());
        return vo;
    }
}
