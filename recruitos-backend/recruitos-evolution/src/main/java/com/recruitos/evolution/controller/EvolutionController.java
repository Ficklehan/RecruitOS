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

/**
 * Evolution management controller
 */
@Api(tags = "Evolution Engine Management")
@RestController
@RequestMapping("/api/evolution")
public class EvolutionController {

    @Resource
    private EvolutionService evolutionService;

    @ApiOperation("Submit a new evolution signal")
    @PostMapping("/signal")
    public R<SignalVO> submitSignal(@Valid @RequestBody SignalSubmitDTO dto) {
        SignalVO vo = evolutionService.submitSignal(dto);
        return R.ok(vo);
    }

    @ApiOperation("Get paginated list of signals with filters")
    @GetMapping("/signal/list")
    public R<PageResult<SignalVO>> getSignalList(SignalQueryDTO query) {
        PageResult<SignalVO> result = evolutionService.getSignalList(query);
        return R.ok(result);
    }

    @ApiOperation("Get current weight snapshot for a job")
    @GetMapping("/weight/{jobId}")
    public R<List<WeightSnapshotVO>> getWeightSnapshot(@PathVariable Long jobId) {
        List<WeightSnapshotVO> voList = evolutionService.getWeightSnapshot(jobId);
        return R.ok(voList);
    }

    @ApiOperation("Get weight change history for a job")
    @GetMapping("/weight/{jobId}/history")
    public R<List<WeightSnapshotVO>> getWeightHistory(@PathVariable Long jobId) {
        List<WeightSnapshotVO> voList = evolutionService.getWeightHistory(jobId);
        return R.ok(voList);
    }

    @ApiOperation("Trigger evolution - simulate weight update based on signals")
    @PostMapping("/trigger/{jobId}")
    public R<List<WeightSnapshotVO>> triggerEvolution(@PathVariable Long jobId) {
        List<WeightSnapshotVO> voList = evolutionService.triggerEvolution(jobId);
        return R.ok(voList);
    }
}
