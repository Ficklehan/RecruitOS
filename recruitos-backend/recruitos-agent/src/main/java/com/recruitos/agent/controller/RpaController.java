package com.recruitos.agent.controller;

import com.recruitos.agent.dto.RpaUnlockDTO;
import com.recruitos.agent.service.RpaLoginService;
import com.recruitos.agent.service.RpaSafetyService;
import com.recruitos.common.result.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    @Resource
    private RpaSafetyService rpaSafetyService;

    @ApiOperation("RPA 运行状态（含测试锁定）")
    @GetMapping("/status")
    public R<Map<String, Object>> status() {
        return R.ok(rpaLoginService.status());
    }

    @ApiOperation("一键锁定：暂停全部寻源 + 禁止访问 Boss/猎聘")
    @PostMapping("/lock-testing")
    public R<Map<String, Object>> lockTesting() {
        return R.ok(rpaSafetyService.lockTestingMode());
    }

    @ApiOperation("开启平台实况联调（须 confirm=true）")
    @PostMapping("/unlock-live")
    public R<Map<String, Object>> unlockLive(@RequestBody RpaUnlockDTO dto) {
        return R.ok(rpaSafetyService.unlockLiveMode(dto));
    }

    @ApiOperation("恢复为 application.yml 默认策略")
    @PostMapping("/reset-access")
    public R<Map<String, Object>> resetAccess() {
        return R.ok(rpaSafetyService.resetToConfig());
    }
}
