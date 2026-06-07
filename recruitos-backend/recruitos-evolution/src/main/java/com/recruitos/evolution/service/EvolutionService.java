package com.recruitos.evolution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruitos.common.auth.CurrentUser;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Evolution service - core business logic for signal processing and weight management
 */
@Service
public class EvolutionService {

    @Resource
    private EvolutionSignalMapper evolutionSignalMapper;

    @Resource
    private JobWeightSnapshotMapper jobWeightSnapshotMapper;

    /**
     * Submit a new evolution signal
     */
    @Transactional
    public SignalVO submitSignal(SignalSubmitDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = CurrentUser.getCurrentUserId();
        if (tenantId == null || userId == null) {
            throw new BizException("User not authenticated");
        }

        EvolutionSignal signal = new EvolutionSignal();
        signal.setTenantId(tenantId);
        signal.setSignalType(dto.getSignalType());
        signal.setJobId(dto.getJobId());
        signal.setJobTitle(dto.getJobTitle());
        signal.setTagId(dto.getTagId());
        signal.setTagName(dto.getTagName());
        signal.setSignalValue(dto.getSignalValue());
        signal.setSource(dto.getSource());
        signal.setProcessed(0);
        signal.setCreatedBy(userId);

        evolutionSignalMapper.insert(signal);

        return convertToSignalVO(signal);
    }

    /**
     * Get paginated list of signals with filters
     */
    public PageResult<SignalVO> getSignalList(SignalQueryDTO query) {
        Long tenantId = TenantContext.getTenantId();

        Page<EvolutionSignal> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<EvolutionSignal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EvolutionSignal::getTenantId, tenantId);

        if (StringUtils.hasText(query.getSignalType())) {
            wrapper.eq(EvolutionSignal::getSignalType, query.getSignalType());
        }
        if (query.getJobId() != null) {
            wrapper.eq(EvolutionSignal::getJobId, query.getJobId());
        }
        if (query.getTagId() != null) {
            wrapper.eq(EvolutionSignal::getTagId, query.getTagId());
        }
        if (query.getProcessed() != null) {
            wrapper.eq(EvolutionSignal::getProcessed, query.getProcessed());
        }

        wrapper.orderByDesc(EvolutionSignal::getCreatedAt);

        Page<EvolutionSignal> result = evolutionSignalMapper.selectPage(page, wrapper);

        List<SignalVO> voList = new ArrayList<>();
        for (EvolutionSignal signal : result.getRecords()) {
            voList.add(convertToSignalVO(signal));
        }

        return new PageResult<>(result.getTotal(), voList, query.getPageNum(), query.getPageSize());
    }

    /**
     * Get current weight snapshot for a job
     */
    public List<WeightSnapshotVO> getWeightSnapshot(Long jobId) {
        Long tenantId = TenantContext.getTenantId();

        LambdaQueryWrapper<JobWeightSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobWeightSnapshot::getTenantId, tenantId)
                .eq(JobWeightSnapshot::getJobId, jobId)
                .orderByDesc(JobWeightSnapshot::getVersion)
                .last("LIMIT 1");

        // Get the latest version
        JobWeightSnapshot latest = jobWeightSnapshotMapper.selectOne(wrapper);
        if (latest == null) {
            return new ArrayList<>();
        }

        // Get all snapshots with the latest version
        LambdaQueryWrapper<JobWeightSnapshot> versionWrapper = new LambdaQueryWrapper<>();
        versionWrapper.eq(JobWeightSnapshot::getTenantId, tenantId)
                .eq(JobWeightSnapshot::getJobId, jobId)
                .eq(JobWeightSnapshot::getVersion, latest.getVersion());

        List<JobWeightSnapshot> snapshots = jobWeightSnapshotMapper.selectList(versionWrapper);

        List<WeightSnapshotVO> voList = new ArrayList<>();
        for (JobWeightSnapshot snapshot : snapshots) {
            voList.add(convertToWeightSnapshotVO(snapshot));
        }

        return voList;
    }

    /**
     * Get weight change history for a job
     */
    public List<WeightSnapshotVO> getWeightHistory(Long jobId) {
        Long tenantId = TenantContext.getTenantId();

        LambdaQueryWrapper<JobWeightSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobWeightSnapshot::getTenantId, tenantId)
                .eq(JobWeightSnapshot::getJobId, jobId)
                .orderByDesc(JobWeightSnapshot::getVersion)
                .orderByAsc(JobWeightSnapshot::getTagId);

        List<JobWeightSnapshot> snapshots = jobWeightSnapshotMapper.selectList(wrapper);

        List<WeightSnapshotVO> voList = new ArrayList<>();
        for (JobWeightSnapshot snapshot : snapshots) {
            voList.add(convertToWeightSnapshotVO(snapshot));
        }

        return voList;
    }

    /**
     * Trigger evolution - simulate weight update based on signals
     * Calculates new tri-weights (match/search/decision) from unprocessed signals
     */
    @Transactional
    public List<WeightSnapshotVO> triggerEvolution(Long jobId) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = CurrentUser.getCurrentUserId();

        // Get unprocessed signals for this job
        LambdaQueryWrapper<EvolutionSignal> signalWrapper = new LambdaQueryWrapper<>();
        signalWrapper.eq(EvolutionSignal::getTenantId, tenantId)
                .eq(EvolutionSignal::getJobId, jobId)
                .eq(EvolutionSignal::getProcessed, 0);

        List<EvolutionSignal> signals = evolutionSignalMapper.selectList(signalWrapper);

        if (signals.isEmpty()) {
            throw new BizException("No unprocessed signals found for job " + jobId);
        }

        // Aggregate signals by tag and type
        Map<Long, Map<String, Double>> tagSignals = new HashMap<>();
        Map<Long, String> tagNames = new HashMap<>();
        String jobTitle = null;

        for (EvolutionSignal signal : signals) {
            if (jobTitle == null && signal.getJobTitle() != null) {
                jobTitle = signal.getJobTitle();
            }
            if (signal.getTagId() != null) {
                tagNames.put(signal.getTagId(), signal.getTagName());
                tagSignals.computeIfAbsent(signal.getTagId(), k -> new HashMap<>())
                        .merge(signal.getSignalType(), signal.getSignalValue(), Double::sum);
            }
        }

        // Get current version
        LambdaQueryWrapper<JobWeightSnapshot> versionWrapper = new LambdaQueryWrapper<>();
        versionWrapper.eq(JobWeightSnapshot::getTenantId, tenantId)
                .eq(JobWeightSnapshot::getJobId, jobId)
                .orderByDesc(JobWeightSnapshot::getVersion)
                .last("LIMIT 1");

        JobWeightSnapshot latestSnapshot = jobWeightSnapshotMapper.selectOne(versionWrapper);
        int newVersion = (latestSnapshot != null) ? latestSnapshot.getVersion() + 1 : 1;

        // Create new weight snapshots based on signal aggregation
        List<WeightSnapshotVO> newSnapshots = new ArrayList<>();

        for (Map.Entry<Long, Map<String, Double>> entry : tagSignals.entrySet()) {
            Long tagId = entry.getKey();
            Map<String, Double> typeValues = entry.getValue();

            // Calculate weights from signals (simple average-based approach)
            double matchWeight = calculateWeight(typeValues.getOrDefault("MATCH", 0.0), 0.5);
            double searchWeight = calculateWeight(typeValues.getOrDefault("SEARCH", 0.0), 0.3);
            double decisionWeight = calculateWeight(typeValues.getOrDefault("DECISION", 0.0), 0.2);

            JobWeightSnapshot snapshot = new JobWeightSnapshot();
            snapshot.setTenantId(tenantId);
            snapshot.setJobId(jobId);
            snapshot.setJobTitle(jobTitle);
            snapshot.setTagId(tagId);
            snapshot.setTagName(tagNames.get(tagId));
            snapshot.setMatchWeight(matchWeight);
            snapshot.setSearchWeight(searchWeight);
            snapshot.setDecisionWeight(decisionWeight);
            snapshot.setVersion(newVersion);
            snapshot.setCreatedBy(userId);

            jobWeightSnapshotMapper.insert(snapshot);
            newSnapshots.add(convertToWeightSnapshotVO(snapshot));
        }

        // Mark signals as processed
        for (EvolutionSignal signal : signals) {
            signal.setProcessed(1);
            evolutionSignalMapper.updateById(signal);
        }

        return newSnapshots;
    }

    /**
     * Calculate weight from signal value with a base weight
     */
    private double calculateWeight(double signalValue, double baseWeight) {
        // Simple weight calculation: base weight adjusted by signal
        double adjusted = baseWeight + (signalValue * 0.1);
        // Clamp to [0.01, 1.0]
        return Math.max(0.01, Math.min(1.0, adjusted));
    }

    /**
     * Convert entity to SignalVO
     */
    private SignalVO convertToSignalVO(EvolutionSignal signal) {
        SignalVO vo = new SignalVO();
        vo.setId(signal.getId());
        vo.setTenantId(signal.getTenantId());
        vo.setSignalType(signal.getSignalType());
        vo.setJobId(signal.getJobId());
        vo.setJobTitle(signal.getJobTitle());
        vo.setTagId(signal.getTagId());
        vo.setTagName(signal.getTagName());
        vo.setSignalValue(signal.getSignalValue());
        vo.setSource(signal.getSource());
        vo.setProcessed(signal.getProcessed());
        vo.setCreatedBy(signal.getCreatedBy());
        vo.setCreatedAt(signal.getCreatedAt());
        vo.setUpdatedAt(signal.getUpdatedAt());
        return vo;
    }

    /**
     * Convert entity to WeightSnapshotVO
     */
    private WeightSnapshotVO convertToWeightSnapshotVO(JobWeightSnapshot snapshot) {
        WeightSnapshotVO vo = new WeightSnapshotVO();
        vo.setId(snapshot.getId());
        vo.setTenantId(snapshot.getTenantId());
        vo.setJobId(snapshot.getJobId());
        vo.setJobTitle(snapshot.getJobTitle());
        vo.setTagId(snapshot.getTagId());
        vo.setTagName(snapshot.getTagName());
        vo.setMatchWeight(snapshot.getMatchWeight());
        vo.setSearchWeight(snapshot.getSearchWeight());
        vo.setDecisionWeight(snapshot.getDecisionWeight());
        vo.setVersion(snapshot.getVersion());
        vo.setCreatedBy(snapshot.getCreatedBy());
        vo.setCreatedAt(snapshot.getCreatedAt());
        return vo;
    }
}
