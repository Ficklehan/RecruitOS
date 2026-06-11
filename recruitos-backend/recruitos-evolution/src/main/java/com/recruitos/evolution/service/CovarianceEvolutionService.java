package com.recruitos.evolution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.evolution.dto.CovarianceVO;
import com.recruitos.evolution.entity.EvolutionSignal;
import com.recruitos.evolution.entity.JobCovarianceMatrix;
import com.recruitos.evolution.mapper.JobCovarianceMatrixMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;

/**
 * Phase 3 协方差进化骨架：记录标签联合波动，对权重更新做收缩（shrinkage）。
 */
@Service
public class CovarianceEvolutionService {

    private static final double DEFAULT_VARIANCE = 0.05;
    private static final double CORRELATION_DECAY = 0.92;

    @Resource
    private JobCovarianceMatrixMapper covarianceMapper;

    @Resource
    private ObjectMapper objectMapper;

    public CovarianceVO getLatest(Long jobId) {
        JobCovarianceMatrix row = latestMatrix(jobId);
        if (row == null) {
            return null;
        }
        CovarianceVO vo = new CovarianceVO();
        vo.setId(row.getId());
        vo.setTenantId(row.getTenantId());
        vo.setJobId(row.getJobId());
        vo.setMatrixData(row.getMatrixData());
        vo.setDimension(row.getDimension());
        vo.setVersion(row.getVersion());
        vo.setCreatedAt(row.getCreatedAt());
        return vo;
    }

    /**
     * 根据历史方差收缩本次 tag 调整幅度，降低高噪声标签的步长。
     */
    public Map<String, Double> applyShrinkage(Long jobId, Map<String, Double> adjustments) {
        if (adjustments == null || adjustments.isEmpty()) {
            return adjustments;
        }
        Map<String, Object> matrix = loadMatrixData(jobId);
        @SuppressWarnings("unchecked")
        Map<String, Double> diagonal = (Map<String, Double>) matrix.getOrDefault("diagonal", Collections.emptyMap());

        Map<String, Double> shrunk = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : adjustments.entrySet()) {
            if (e.getKey().startsWith("_")) {
                shrunk.put(e.getKey(), e.getValue());
                continue;
            }
            double variance = diagonal.getOrDefault(e.getKey(), DEFAULT_VARIANCE);
            double factor = 1.0 / (1.0 + Math.sqrt(variance));
            shrunk.put(e.getKey(), e.getValue() * factor);
        }
        return shrunk;
    }

    @Transactional
    public void recordSignalBatch(Long jobId, List<EvolutionSignal> signals, Map<String, Double> adjustments) {
        if (jobId == null || adjustments == null || adjustments.isEmpty()) {
            return;
        }
        Long tenantId = TenantContext.getTenantId();
        Map<String, Object> matrix = loadMatrixData(jobId);
        @SuppressWarnings("unchecked")
        Map<String, Double> diagonal = new LinkedHashMap<>((Map<String, Double>) matrix.getOrDefault("diagonal", new LinkedHashMap<>()));
        @SuppressWarnings("unchecked")
        Map<String, Double> correlations = new LinkedHashMap<>((Map<String, Double>) matrix.getOrDefault("correlations", new LinkedHashMap<>()));

        Set<String> tags = new LinkedHashSet<>();
        for (String key : adjustments.keySet()) {
            if (!key.startsWith("_")) {
                tags.add(key);
            }
        }
        for (EvolutionSignal s : signals) {
            mergeSignalTags(tags, s);
        }

        int n = Math.max(1, signals.size());
        for (String tag : tags) {
            double prev = diagonal.getOrDefault(tag, DEFAULT_VARIANCE);
            diagonal.put(tag, prev * CORRELATION_DECAY + 0.01 / n);
        }
        List<String> tagList = new ArrayList<>(tags);
        for (int i = 0; i < tagList.size(); i++) {
            for (int j = i + 1; j < tagList.size(); j++) {
                String a = tagList.get(i);
                String b = tagList.get(j);
                String pairKey = pairKey(a, b);
                double prev = correlations.getOrDefault(pairKey, 0.0);
                double bump = adjustments.containsKey(a) && adjustments.containsKey(b) ? 0.08 / n : 0.02 / n;
                correlations.put(pairKey, clamp(prev * CORRELATION_DECAY + bump, -0.95, 0.95));
            }
        }

        matrix.put("tags", tagList);
        matrix.put("diagonal", diagonal);
        matrix.put("correlations", correlations);
        matrix.put("signalCount", n);

        try {
            String json = objectMapper.writeValueAsString(matrix);
            JobCovarianceMatrix existing = latestMatrix(jobId);
            if (existing == null) {
                JobCovarianceMatrix row = new JobCovarianceMatrix();
                row.setTenantId(tenantId);
                row.setJobId(jobId);
                row.setMatrixData(json);
                row.setDimension(tagList.size());
                row.setVersion(1);
                covarianceMapper.insert(row);
            } else {
                existing.setMatrixData(json);
                existing.setDimension(tagList.size());
                existing.setVersion(existing.getVersion() != null ? existing.getVersion() + 1 : 2);
                covarianceMapper.updateById(existing);
            }
        } catch (Exception ignored) {
            // non-blocking
        }
    }

    private void mergeSignalTags(Set<String> tags, EvolutionSignal signal) {
        if (!StringUtils.hasText(signal.getTagAdjustments())) {
            return;
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(signal.getTagAdjustments(),
                    new TypeReference<Map<String, Object>>() {});
            for (String key : raw.keySet()) {
                if (!key.startsWith("_")) {
                    tags.add(key);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private Map<String, Object> loadMatrixData(Long jobId) {
        JobCovarianceMatrix row = latestMatrix(jobId);
        if (row == null || !StringUtils.hasText(row.getMatrixData())) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(row.getMatrixData(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private JobCovarianceMatrix latestMatrix(Long jobId) {
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<JobCovarianceMatrix> w = new LambdaQueryWrapper<>();
        w.eq(JobCovarianceMatrix::getTenantId, tenantId)
                .eq(JobCovarianceMatrix::getJobId, jobId)
                .orderByDesc(JobCovarianceMatrix::getVersion)
                .orderByDesc(JobCovarianceMatrix::getCreatedAt)
                .last("LIMIT 1");
        return covarianceMapper.selectOne(w);
    }

    private static String pairKey(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
