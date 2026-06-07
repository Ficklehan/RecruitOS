package com.recruitos.analytics.controller;

import com.recruitos.analytics.dto.FunnelVO;
import com.recruitos.analytics.service.FunnelService;
import com.recruitos.common.result.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Funnel analytics controller
 */
@Api(tags = "Funnel Analytics")
@RestController
@RequestMapping("/api/analytics/funnel")
public class FunnelController {

    @Resource
    private FunnelService funnelService;

    @ApiOperation("Get recruitment funnel data")
    @GetMapping
    public R<FunnelVO> getFunnelData(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        FunnelVO vo = funnelService.getFunnelData(dateFrom, dateTo);
        return R.ok(vo);
    }
}
