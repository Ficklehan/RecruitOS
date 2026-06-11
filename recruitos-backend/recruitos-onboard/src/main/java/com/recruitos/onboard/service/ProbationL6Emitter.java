package com.recruitos.onboard.service;

import com.recruitos.common.evolution.ModuleEvolutionEmitter;
import com.recruitos.onboard.entity.ProbationCheckin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 试用期验证→L6进化信号发射器。
 * 替代旧Demo Webhook，将真实试用期评估结果回流进化引擎。
 */
@Component
public class ProbationL6Emitter {
    private static final Logger log = LoggerFactory.getLogger(ProbationL6Emitter.class);

    @Resource
    private ModuleEvolutionEmitter moduleEvolutionEmitter;

    /**
     * 试用期check-in提交后，发射L6信号到进化引擎。
     * 信号权重按checkin_day阶梯增长：
     * - Day 30: weight=0.3（早期信号，置信度低）
     * - Day 60: weight=0.6
     * - Day 90: weight=1.0（转正评估，置信度最高）
     */
    public void emitL6Signal(ProbationCheckin checkin) {
        if (checkin == null || Boolean.TRUE.equals(checkin.getL6SignalEmitted())) {
            return;
        }

        double weight = resolveWeight(checkin.getCheckinDay());

        try {
            String decision = resolveDecision(checkin);
            boolean passed = "PASS".equals(decision);
            Integer score = checkin.getOverallRating() != null
                ? (int) Math.round(checkin.getOverallRating() * 20) // 1-5 → 20-100
                : null;
            moduleEvolutionEmitter.emitProbationResult(
                checkin.getJobId(),
                checkin.getCandidateId(),
                passed,
                score
            );
            log.info("L6 signal emitted: job={}, candidate={}, day={}, rating={}, weight={}",
                checkin.getJobId(), checkin.getCandidateId(),
                checkin.getCheckinDay(), checkin.getOverallRating(), weight);
        } catch (Exception e) {
            log.warn("L6 signal emission failed: {}", e.getMessage());
        }
    }

    private double resolveWeight(Integer day) {
        if (day == null) return 1.0;
        if (day <= 30) return 0.3;
        if (day <= 60) return 0.6;
        return 1.0;
    }

    private String resolveDecision(ProbationCheckin checkin) {
        if (checkin.getOverallRating() == null) return "PENDING";
        return checkin.getOverallRating() >= 3.5 ? "PASS" : "FAIL";
    }
}
