package com.recruitos.evolution.controller;

import com.recruitos.common.result.R;
import com.recruitos.evolution.dto.CovarianceVO;
import com.recruitos.evolution.service.CovarianceEvolutionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Api(tags = "Evolution Covariance (Phase 3)")
@RestController
@RequestMapping("/api/evolution/covariance")
public class CovarianceController {

    @Resource
    private CovarianceEvolutionService covarianceEvolutionService;

    @ApiOperation("获取岗位最新协方差矩阵")
    @GetMapping("/{jobId}")
    public R<CovarianceVO> getCovariance(@PathVariable Long jobId) {
        CovarianceVO vo = covarianceEvolutionService.getLatest(jobId);
        return R.ok(vo);
    }
}
