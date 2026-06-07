package com.recruitos.evolution.controller;

import com.recruitos.common.result.R;
import com.recruitos.evolution.dto.HealthVO;
import com.recruitos.evolution.service.HealthService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * Health monitoring controller
 */
@Api(tags = "Evolution Health Monitoring")
@RestController
@RequestMapping("/api/evolution/health")
public class HealthController {

    @Resource
    private HealthService healthService;

    @ApiOperation("Get health score for a specific job")
    @GetMapping("/job/{id}")
    public R<HealthVO> getJobHealth(@PathVariable Long id) {
        HealthVO vo = healthService.getJobHealth(id);
        return R.ok(vo);
    }

    @ApiOperation("Get overall system health across all jobs")
    @GetMapping("/system")
    public R<HealthVO> getSystemHealth() {
        HealthVO vo = healthService.getSystemHealth();
        return R.ok(vo);
    }

    @ApiOperation("Get list of jobs with health issues")
    @GetMapping("/alerts")
    public R<List<HealthVO>> getHealthAlerts() {
        List<HealthVO> alerts = healthService.getHealthAlerts();
        return R.ok(alerts);
    }
}
