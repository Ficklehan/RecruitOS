package com.recruitos.analytics.controller;

import com.recruitos.analytics.dto.RoiVO;
import com.recruitos.analytics.service.RoiService;
import com.recruitos.common.result.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * ROI analytics controller
 */
@Api(tags = "ROI Analytics")
@RestController
@RequestMapping("/api/analytics/roi")
public class RoiController {

    @Resource
    private RoiService roiService;

    @ApiOperation("Get recruitment ROI data by channel")
    @GetMapping
    public R<RoiVO> getRoiData(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        RoiVO vo = roiService.getRoiData(dateFrom, dateTo);
        return R.ok(vo);
    }
}
