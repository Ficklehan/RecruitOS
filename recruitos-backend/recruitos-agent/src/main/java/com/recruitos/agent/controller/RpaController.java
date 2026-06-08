package com.recruitos.agent.controller;

import com.recruitos.agent.service.RpaLoginService;
import com.recruitos.common.result.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

@Api(tags = "RPA Runtime")
@RestController
@RequestMapping("/api/agent/rpa")
public class RpaController {

    @Resource
    private RpaLoginService rpaLoginService;

    @ApiOperation("RPA runtime status")
    @GetMapping("/status")
    public R<Map<String, Object>> status() {
        return R.ok(rpaLoginService.status());
    }
}
