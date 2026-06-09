package com.recruitos.evolution.controller;

import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import com.recruitos.evolution.dto.*;
import com.recruitos.evolution.service.EvolutionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@Api(tags = "Evolution Engine Management")
@RestController
@RequestMapping("/api/evolution")
public class EvolutionController {

    @Resource
    private EvolutionService evolutionService;

    @ApiOperation("Emit evolution signal (internal / cross-module)")
    @PostMapping("/signals/emit")
    public R<SignalVO> emitSignal(@Valid @RequestBody SignalEmitDTO dto) {
        return R.ok(evolutionService.emitSignal(dto));
    }

    @ApiOperation("Submit a new evolution signal (legacy)")
    @PostMapping("/signal")
    @Deprecated
    public R<SignalVO> submitSignal(@Valid @RequestBody SignalSubmitDTO dto) {
        return R.ok(evolutionService.submitSignal(dto));
    }

    @ApiOperation("Get paginated list of signals with filters")
    @GetMapping("/signal/list")
    public R<PageResult<SignalVO>> getSignalList(SignalQueryDTO query) {
        return R.ok(evolutionService.getSignalList(query));
    }

    @ApiOperation("Get current weight snapshot for a job")
    @GetMapping("/weight/{jobId}")
    public R<List<WeightSnapshotVO>> getWeightSnapshot(@PathVariable Long jobId) {
        return R.ok(evolutionService.getWeightSnapshot(jobId));
    }

    @ApiOperation("Get weight change history for a job")
    @GetMapping("/weight/{jobId}/history")
    public R<List<WeightSnapshotVO>> getWeightHistory(@PathVariable Long jobId) {
        return R.ok(evolutionService.getWeightHistory(jobId));
    }

    @ApiOperation("Trigger evolution from pending signals")
    @PostMapping("/trigger/{jobId}")
    public R<List<WeightSnapshotVO>> triggerEvolution(@PathVariable Long jobId) {
        return R.ok(evolutionService.triggerEvolution(jobId));
    }
}
