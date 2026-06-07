package com.recruitos.analytics.controller;

import com.recruitos.analytics.dto.CycleVO;
import com.recruitos.analytics.service.CycleService;
import com.recruitos.common.result.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Cycle time analytics controller
 */
@Api(tags = "Cycle Time Analytics")
@RestController
@RequestMapping("/api/analytics/cycle")
public class CycleController {

    @Resource
    private CycleService cycleService;

    @ApiOperation("Get recruitment cycle time data")
    @GetMapping
    public R<CycleVO> getCycleData(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        CycleVO vo = cycleService.getCycleData(dateFrom, dateTo);
        return R.ok(vo);
    }
}
