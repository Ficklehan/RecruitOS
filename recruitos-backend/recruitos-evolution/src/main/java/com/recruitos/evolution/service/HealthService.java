package com.recruitos.evolution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recruitos.evolution.dto.HealthVO;
import com.recruitos.evolution.entity.EvolutionSignal;
import com.recruitos.evolution.entity.JobWeightSnapshot;
import com.recruitos.evolution.mapper.EvolutionSignalMapper;
import com.recruitos.evolution.mapper.JobWeightSnapshotMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class HealthService {

    private static final int SIGNAL_THRESHOLD = 10;
    private static final int FRESHNESS_THRESHOLD_DAYS = 30;

    @Resource
    private EvolutionSignalMapper evolutionSignalMapper;

    @Resource
    private JobWeightSnapshotMapper jobWeightSnapshotMapper;

    public HealthVO getJobHealth(Long jobId) {
        Long tenantId = TenantContextHelper.requireTenantId();

        JobWeightSnapshot latestSnapshot = latestWeightSnapshot(tenantId, jobId);

        long signalCount = countSignals(tenantId, jobId, null);
        int dataSufficiencyScore = calculateDataSufficiencyScore(signalCount);

        List<JobWeightSnapshot> recentSnapshots = recentSnapshots(tenantId, jobId, 10);
        int weightStabilityScore = calculateWeightStabilityScore(recentSnapshots);

        long l3Count = countSignals(tenantId, jobId, 3);
        int matchQualityScore = calculateMatchQualityScore(l3Count);

        EvolutionSignal latestSignal = latestSignal(tenantId, jobId);
        int evolutionFreshnessScore = calculateEvolutionFreshnessScore(latestSignal);

        int overallScore = (int) (dataSufficiencyScore * 0.25
                + weightStabilityScore * 0.25
                + matchQualityScore * 0.25
                + evolutionFreshnessScore * 0.25);

        String status = overallScore >= 80 ? "HEALTHY" : overallScore >= 60 ? "WARNING" : "CRITICAL";
        List<String> alerts = generateAlerts(jobId, dataSufficiencyScore, weightStabilityScore,
                matchQualityScore, evolutionFreshnessScore);

        HealthVO vo = new HealthVO();
        vo.setJobId(jobId);
        vo.setJobTitle(latestSnapshot != null ? "Job-" + jobId : "Unknown");
        vo.setOverallScore(overallScore);
        vo.setDataSufficiencyScore(dataSufficiencyScore);
        vo.setWeightStabilityScore(weightStabilityScore);
        vo.setMatchQualityScore(matchQualityScore);
        vo.setEvolutionFreshnessScore(evolutionFreshnessScore);
        vo.setStatus(status);
        vo.setAlerts(alerts);
        return vo;
    }

    public HealthVO getSystemHealth() {
        Long tenantId = TenantContextHelper.requireTenantId();
        LambdaQueryWrapper<JobWeightSnapshot> jobWrapper = new LambdaQueryWrapper<>();
        jobWrapper.eq(JobWeightSnapshot::getTenantId, tenantId)
                .select(JobWeightSnapshot::getJobId)
                .groupBy(JobWeightSnapshot::getJobId);
        List<JobWeightSnapshot> jobSnapshots = jobWeightSnapshotMapper.selectList(jobWrapper);

        if (jobSnapshots.isEmpty()) {
            HealthVO vo = new HealthVO();
            vo.setOverallScore(0);
            vo.setStatus("CRITICAL");
            vo.setAlerts(Collections.singletonList("No jobs with evolution data found"));
            return vo;
        }

        int totalDataSufficiency = 0;
        int totalWeightStability = 0;
        int totalMatchQuality = 0;
        int totalEvolutionFreshness = 0;
        int jobCount = jobSnapshots.size();

        for (JobWeightSnapshot snapshot : jobSnapshots) {
            try {
                HealthVO jobHealth = getJobHealth(snapshot.getJobId());
                totalDataSufficiency += jobHealth.getDataSufficiencyScore();
                totalWeightStability += jobHealth.getWeightStabilityScore();
                totalMatchQuality += jobHealth.getMatchQualityScore();
                totalEvolutionFreshness += jobHealth.getEvolutionFreshnessScore();
            } catch (Exception ignored) {
                // skip
            }
        }

        int avgDataSufficiency = totalDataSufficiency / jobCount;
        int avgWeightStability = totalWeightStability / jobCount;
        int avgMatchQuality = totalMatchQuality / jobCount;
        int avgEvolutionFreshness = totalEvolutionFreshness / jobCount;

        int overallScore = (int) (avgDataSufficiency * 0.25
                + avgWeightStability * 0.25
                + avgMatchQuality * 0.25
                + avgEvolutionFreshness * 0.25);

        String status = overallScore >= 80 ? "HEALTHY" : overallScore >= 60 ? "WARNING" : "CRITICAL";

        List<String> alerts = new ArrayList<>();
        if (avgDataSufficiency < 60) {
            alerts.add("Low data sufficiency across system");
        }
        if (avgWeightStability < 60) {
            alerts.add("Weight instability detected across system");
        }
        if (avgMatchQuality < 60) {
            alerts.add("Low match quality across system");
        }
        if (avgEvolutionFreshness < 60) {
            alerts.add("Evolution data freshness issues across system");
        }

        HealthVO vo = new HealthVO();
        vo.setOverallScore(overallScore);
        vo.setDataSufficiencyScore(avgDataSufficiency);
        vo.setWeightStabilityScore(avgWeightStability);
        vo.setMatchQualityScore(avgMatchQuality);
        vo.setEvolutionFreshnessScore(avgEvolutionFreshness);
        vo.setStatus(status);
        vo.setAlerts(alerts);
        return vo;
    }

    public List<HealthVO> getHealthAlerts() {
        Long tenantId = TenantContextHelper.requireTenantId();
        LambdaQueryWrapper<JobWeightSnapshot> jobWrapper = new LambdaQueryWrapper<>();
        jobWrapper.eq(JobWeightSnapshot::getTenantId, tenantId)
                .select(JobWeightSnapshot::getJobId)
                .groupBy(JobWeightSnapshot::getJobId);
        List<HealthVO> alerts = new ArrayList<>();
        for (JobWeightSnapshot snapshot : jobWeightSnapshotMapper.selectList(jobWrapper)) {
            try {
                HealthVO jobHealth = getJobHealth(snapshot.getJobId());
                if (!"HEALTHY".equals(jobHealth.getStatus())) {
                    alerts.add(jobHealth);
                }
            } catch (Exception ignored) {
                // skip
            }
        }
        return alerts;
    }

    private JobWeightSnapshot latestWeightSnapshot(Long tenantId, Long jobId) {
        LambdaQueryWrapper<JobWeightSnapshot> w = new LambdaQueryWrapper<>();
        w.eq(JobWeightSnapshot::getTenantId, tenantId)
                .eq(JobWeightSnapshot::getJobId, jobId)
                .orderByDesc(JobWeightSnapshot::getCreatedAt)
                .last("LIMIT 1");
        return jobWeightSnapshotMapper.selectOne(w);
    }

    private List<JobWeightSnapshot> recentSnapshots(Long tenantId, Long jobId, int limit) {
        LambdaQueryWrapper<JobWeightSnapshot> w = new LambdaQueryWrapper<>();
        w.eq(JobWeightSnapshot::getTenantId, tenantId)
                .eq(JobWeightSnapshot::getJobId, jobId)
                .orderByDesc(JobWeightSnapshot::getCreatedAt)
                .last("LIMIT " + limit);
        return jobWeightSnapshotMapper.selectList(w);
    }

    private long countSignals(Long tenantId, Long jobId, Integer level) {
        LambdaQueryWrapper<EvolutionSignal> w = new LambdaQueryWrapper<>();
        w.eq(EvolutionSignal::getTenantId, tenantId).eq(EvolutionSignal::getJobId, jobId);
        if (level != null) {
            w.eq(EvolutionSignal::getSignalLevel, level);
        }
        return evolutionSignalMapper.selectCount(w);
    }

    private EvolutionSignal latestSignal(Long tenantId, Long jobId) {
        LambdaQueryWrapper<EvolutionSignal> w = new LambdaQueryWrapper<>();
        w.eq(EvolutionSignal::getTenantId, tenantId)
                .eq(EvolutionSignal::getJobId, jobId)
                .orderByDesc(EvolutionSignal::getCreatedAt)
                .last("LIMIT 1");
        return evolutionSignalMapper.selectOne(w);
    }

    private int calculateDataSufficiencyScore(long signalCount) {
        if (signalCount >= SIGNAL_THRESHOLD * 5) {
            return 100;
        } else if (signalCount >= SIGNAL_THRESHOLD * 2) {
            return 80;
        } else if (signalCount >= SIGNAL_THRESHOLD) {
            return 60;
        } else if (signalCount >= SIGNAL_THRESHOLD / 2) {
            return 40;
        }
        return 20;
    }

    private int calculateWeightStabilityScore(List<JobWeightSnapshot> snapshots) {
        if (snapshots.size() < 2) {
            return 50;
        }
        return 75;
    }

    private int calculateMatchQualityScore(long signalCount) {
        if (signalCount >= 100) {
            return 100;
        } else if (signalCount >= 50) {
            return 80;
        } else if (signalCount >= 20) {
            return 60;
        } else if (signalCount >= 10) {
            return 40;
        }
        return 20;
    }

    private int calculateEvolutionFreshnessScore(EvolutionSignal latestSignal) {
        if (latestSignal == null || latestSignal.getCreatedAt() == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(latestSignal.getCreatedAt(), LocalDateTime.now());
        if (days <= 1) {
            return 100;
        } else if (days <= 7) {
            return 80;
        } else if (days <= 14) {
            return 60;
        } else if (days <= FRESHNESS_THRESHOLD_DAYS) {
            return 40;
        }
        return 20;
    }

    private List<String> generateAlerts(Long jobId, int dataSufficiency, int weightStability,
                                          int matchQuality, int evolutionFreshness) {
        List<String> alerts = new ArrayList<>();
        if (dataSufficiency < 60) {
            alerts.add("Insufficient signal data for job " + jobId);
        }
        if (weightStability < 60) {
            alerts.add("Weight instability detected for job " + jobId);
        }
        if (matchQuality < 60) {
            alerts.add("Low match quality for job " + jobId);
        }
        if (evolutionFreshness < 60) {
            alerts.add("Evolution data is stale for job " + jobId);
        }
        return alerts;
    }

    /** 避免 HealthService 直接依赖 TenantContext 静态方法时的循环引用 */
    private static final class TenantContextHelper {
        private TenantContextHelper() {
        }

        static Long requireTenantId() {
            Long tenantId = com.recruitos.common.tenant.TenantContext.getTenantId();
            if (tenantId == null) {
                throw new com.recruitos.common.exception.BizException("Tenant context required");
            }
            return tenantId;
        }
    }
}
