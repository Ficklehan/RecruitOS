package com.recruitos.evolution.controller;

import com.recruitos.common.result.R;
import com.recruitos.evolution.service.EvolutionSettingsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@Api(tags = "Evolution Settings")
@RestController
@RequestMapping("/api/evolution/settings")
public class EvolutionSettingsController {

    @Resource
    private EvolutionSettingsService settingsService;

    @ApiOperation("获取进化引擎租户设置")
    @GetMapping
    public R<Map<String, Object>> getSettings() {
        return R.ok(settingsService.getSettings());
    }

    @ApiOperation("更新生成建议所需最少信号数（试用友好）")
    @PutMapping("/min-signals")
    public R<Map<String, Object>> updateMinSignals(@RequestBody Map<String, Integer> body) {
        Integer minSignals = body != null ? body.get("minSignals") : null;
        if (minSignals == null) {
            minSignals = body != null ? body.get("min_signals") : null;
        }
        if (minSignals == null) {
            throw new com.recruitos.common.exception.BizException("缺少 minSignals");
        }
        return R.ok(settingsService.updateMinSignals(minSignals));
    }
}
