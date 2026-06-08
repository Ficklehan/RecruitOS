package com.recruitos.candidate.controller;

import com.recruitos.candidate.dto.*;
import com.recruitos.candidate.entity.HiringDecision;
import com.recruitos.candidate.service.PipelineService;
import com.recruitos.common.result.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@Api(tags = "Pipeline")
@RestController
@RequestMapping("/api/pipeline")
public class PipelineController {

    @Resource
    private PipelineService pipelineService;

    @ApiOperation("Get pipeline kanban board")
    @GetMapping("/board")
    public R<PipelineBoardVO> getBoard(@RequestParam(required = false) Long jobId) {
        return R.ok(pipelineService.getBoard(jobId));
    }

    @ApiOperation("Advance pipeline stage")
    @PutMapping("/candidate-jobs/{id}/stage")
    public R<CandidateJobVO> advanceStage(@PathVariable Long id, @RequestBody PipelineAdvanceDTO dto) {
        return R.ok(pipelineService.advanceStage(id, dto));
    }

    @ApiOperation("Candidate 360 view")
    @GetMapping("/candidates/{id}/360")
    public R<Map<String, Object>> getCandidate360(@PathVariable Long id) {
        return R.ok(pipelineService.getCandidate360(id));
    }

    @ApiOperation("Save hiring decision")
    @PostMapping("/hiring-decisions")
    public R<HiringDecision> saveHiringDecision(@RequestBody HiringDecisionDTO dto) {
        return R.ok(pipelineService.saveHiringDecision(dto));
    }
}
