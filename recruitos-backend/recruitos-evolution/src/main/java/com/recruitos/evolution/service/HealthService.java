package com.recruitos.evolution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.tenant.TenantContext;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Health service - monitors evolution system health
 */
@Service
public class HealthService {

    @Resource
    private EvolutionSignalMapper evolutionSignalMapper;

    @Resource
    private JobWeightSnapshotMapper jobWeightSnapshotMapper;

    /** Minimum signal count threshold for data sufficiency */
    private static final int SIGNAL_THRESHOLD = 10;

    /** Maximum days without update for freshness */
    private static final int FRESHNESS_THRESHOLD_DAYS = 30;

    /**
     * Calculate 4-dimension health score for a job
     */
    public HealthVO getJobHealth(Long jobId) {
        Long tenantId = TenantContext.getTenantId();

        // Get job title from latest snapshot
        LambdaQueryWrapper<JobWeightSnapshot> snapshotWrapper = new LambdaQueryWrapper<>();
        snapshotWrapper.eq(JobWeightSnapshot::getTenantId, tenantId)
                .eq(JobWeightSnapshot::getJobId, jobId)
                .orderByDesc(JobWeightSnapshot::getCreatedAt)
                .last("LIMIT 1");
        JobWeightSnapshot latestSnapshot = jobWeightSnapshotMapper.selectOne(snapshotWrapper);

        // Calculate signal count for data sufficiency
        LambdaQueryWrapper<EvolutionSignal> signalCountWrapper = new LambdaQueryWrapper<>();
        signalCountWrapper.eq(EvolutionSignal::getTenantId, tenantId)
                .eq(EvolutionSignal::getJobId, jobId);
        long signalCount = evolutionSignalMapper.selectCount(signalCountWrapper);
        int dataSufficiencyScore = calculateDataSufficiencyScore(signalCount);

        // Calculate weight stability score
        LambdaQueryWrapper<JobWeightSnapshot> allSnapshotsWrapper = new LambdaQueryWrapper<>();
        allSnapshotsWrapper.eq(JobWeightSnapshot::getTenantId, tenantId)
                .eq(JobWeightSnapshot::getJobId, jobId)
                .orderByDesc(JobWeightSnapshot::getVersion)
                .last("LIMIT 10");
        List<JobWeightSnapshot> recentSnapshots = jobWeightSnapshotMapper.selectList(allSnapshotsWrapper);
        int weightStabilityScore = calculateWeightStabilityScore(recentSnapshots);

        // Calculate match quality score (average match weight)
        LambdaQueryWrapper<EvolutionSignal> matchWrapper = new LambdaQueryWrapper<>();
        matchWrapper.eq(EvolutionSignal::getTenantId, tenantId)
                .eq(EvolutionSignal::getJobId, jobId)
                .eq(EvolutionSignal::getSignalType, "MATCH");
        long matchSignalCount = evolutionSignalMapper.selectCount(matchWrapper);
        int matchQualityScore = calculateMatchQualityScore(matchSignalCount);

        // Calculate evolution freshness score
        LambdaQueryWrapper<EvolutionSignal> latestSignalWrapper = new LambdaQueryWrapper<>();
        latestSignalWrapper.eq(EvolutionSignal::getTenantId, tenantId)
                .eq(EvolutionSignal::getJobId, jobId)
                .orderByDesc(EvolutionSignal::getCreatedAt)
                .last("LIMIT 1");
        EvolutionSignal latestSignal = evolutionSignalMapper.selectOne(latestSignalWrapper);
        int evolutionFreshnessScore = calculateEvolutionFreshnessScore(latestSignal);

        // Calculate overall score (weighted average)
        int overallScore = (int) (dataSufficiencyScore * 0.25 +
                weightStabilityScore * 0.25 +
                matchQualityScore * 0.25 +
                evolutionFreshnessScore * 0.25);

        // Determine status
        String status;
        if (overallScore >= 80) {
            status = "HEALTHY";
        } else if (overallScore >= 60) {
            status = "WARNING";
        } else {
            status = "CRITICAL";
        }

        // Generate alerts
        List<String> alerts = generateAlerts(jobId, dataSufficiencyScore, weightStabilityScore,
                matchQualityScore, evolutionFreshnessScore);

        HealthVO vo = new HealthVO();
        vo.setJobId(jobId);
        vo.setJobTitle(latestSnapshot != null ? latestSnapshot.getJobTitle() : "Unknown");
        vo.setOverallScore(overallScore);
        vo.setDataSufficiencyScore(dataSufficiencyScore);
        vo.setWeightStabilityScore(weightStabilityScore);
        vo.setMatchQualityScore(matchQualityScore);
        vo.setEvolutionFreshnessScore(evolutionFreshnessScore);
        vo.setStatus(status);
        vo.setAlerts(alerts);

        return vo;
    }

    /**
     * Get overall system health across all jobs
     */
    public HealthVO getSystemHealth() {
        Long tenantId = TenantContext.getTenantId();

        // Get all unique job IDs
        LambdaQueryWrapper<JobWeightSnapshot> jobWrapper = new LambdaQueryWrapper<>();
        jobWrapper.eq(JobWeightSnapshot::getTenantId, tenantId)
                .select(JobWeightSnapshot::getJobId)
                .groupBy(JobWeightSnapshot::getJobId);
        List<JobWeightSnapshot> jobSnapshots = jobWeightSnapshotMapper.selectList(jobWrapper);

        if (jobSnapshots.isEmpty()) {
            HealthVO vo = new HealthVO();
            vo.setOverallScore(0);
            vo.setStatus("CRITICAL");
            vo.setAlerts(List.of("No jobs with evolution data found"));
            return vo;
        }

        // Calculate average scores across all jobs
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
            } catch (Exception e) {
                // Skip jobs that fail health check
            }
        }

        int avgDataSufficiency = totalDataSufficiency / jobCount;
        int avgWeightStability = totalWeightStability / jobCount;
        int avgMatchQuality = totalMatchQuality / jobCount;
        int avgEvolutionFreshness = totalEvolutionFreshness / jobCount;

        int overallScore = (int) (avgDataSufficiency * 0.25 +
                avgWeightStability * 0.25 +
                avgMatchQuality * 0.25 +
                avgEvolutionFreshness * 0.25);

        String status;
        if (overallScore >= 80) {
            status = "HEALTHY";
        } else if (overallScore >= 60) {
            status = "WARNING";
        } else {
            status = "CRITICAL";
        }

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

    /**
     * Get list of jobs with health issues
     */
    public List<HealthVO> getHealthAlerts() {
        Long tenantId = TenantContext.getTenantId();

        // Get all unique job IDs
        LambdaQueryWrapper<JobWeightSnapshot> jobWrapper = new LambdaQueryWrapper<>();
        jobWrapper.eq(JobWeightSnapshot::getTenantId, tenantId)
                .select(JobWeightSnapshot::getJobId)
                .groupBy(JobWeightSnapshot::getJobId);
        List<JobWeightSnapshot> jobSnapshots = jobWeightSnapshotMapper.selectList(jobWrapper);

        List<HealthVO> alerts = new ArrayList<>();

        for (JobWeightSnapshot snapshot : jobSnapshots) {
            try {
                HealthVO jobHealth = getJobHealth(snapshot.getJobId());
                if (!"HEALTHY".equals(jobHealth.getStatus())) {
                    alerts.add(jobHealth);
                }
            } catch (Exception e) {
                // Skip jobs that fail health check
            }
        }

        return alerts;
    }

    /**
     * Calculate data sufficiency score based on signal count
     */
    private int calculateDataSufficiencyScore(long signalCount) {
        if (signalCount >= SIGNAL_THRESHOLD * 5) {
            return 100;
        } else if (signalCount >= SIGNAL_THRESHOLD * 2) {
            return 80;
        } else if (signalCount >= SIGNAL_THRESHOLD) {
            return 60;
        } else if (signalCount >= SIGNAL_THRESHOLD / 2) {
            return 40;
        } else {
            return 20;
        }
    }

    /**
     * Calculate weight stability score based on weight variance
     */
    private int calculateWeightStabilityScore(List<JobWeightSnapshot> snapshots) {
        if (snapshots.size() < 2) {
            return 50; // Not enough data
        }

        // Calculate variance of match weights
        double sum = 0;
        double sumSquared = 0;
        int count = 0;

        for (JobWeightSnapshot snapshot : snapshots) {
            if (snapshot.getMatchWeight() != null) {
                sum += snapshot.getMatchWeight();
                sumSquared += snapshot.getMatchWeight() * snapshot.getMatchWeight();
                count++;
            }
        }

        if (count < 2) {
            return 50;
        }

        double mean = sum / count;
        double variance = (sumSquared / count) - (mean * mean);
        double stdDev = Math.sqrt(Math.abs(variance));

        // Lower variance = higher score
        if (stdDev < 0.05) {
            return 100;
        } else if (stdDev < 0.1) {
            return 80;
        } else if (stdDev < 0.2) {
            return 60;
        } else if (stdDev < 0.3) {
            return 40;
        } else {
            return 20;
        }
    }

    /**
     * Calculate match quality score based on match signal count
     */
    private int calculateMatchQualityScore(long matchSignalCount) {
        if (matchSignalCount >= 100) {
            return 100;
        } else if (matchSignalCount >= 50) {
            return 80;
        } else if (matchSignalCount >= 20) {
            return 60;
        } else if (matchSignalCount >= 10) {
            return 40;
        } else {
            return 20;
        }
    }

    /**
     * Calculate evolution freshness score based on days since last signal
     */
    private int calculateEvolutionFreshnessScore(EvolutionSignal latestSignal) {
        if (latestSignal == null || latestSignal.getCreatedAt() == null) {
            return 0;
        }

        long daysSinceUpdate = ChronoUnit.DAYS.between(latestSignal.getCreatedAt(), LocalDateTime.now());

        if (daysSinceUpdate <= 1) {
            return 100;
        } else if (daysSinceUpdate <= 7) {
            return 80;
        } else if (daysSinceUpdate <= 14) {
            return 60;
        } else if (daysSinceUpdate <= FRESHNESS_THRESHOLD_DAYS) {
            return 40;
        } else {
            return 20;
        }
    }

    /**
     * Generate alerts based on health scores
     */
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
}
