package com.recruitos.candidate.controller;

import com.recruitos.candidate.dto.*;
import com.recruitos.candidate.service.CandidateService;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * Candidate management controller
 */
@Api(tags = "Candidate Management")
@RestController
@RequestMapping("/api/candidate")
public class CandidateController {

    @Resource
    private CandidateService candidateService;

    @ApiOperation("Create a new candidate")
    @PostMapping
    public R<CandidateVO> createCandidate(@Valid @RequestBody CandidateCreateDTO dto) {
        CandidateVO vo = candidateService.createCandidate(dto);
        return R.ok(vo);
    }

    @ApiOperation("Update a candidate")
    @PutMapping("/{id}")
    public R<CandidateVO> updateCandidate(@PathVariable Long id, @Valid @RequestBody CandidateCreateDTO dto) {
        CandidateVO vo = candidateService.updateCandidate(id, dto);
        return R.ok(vo);
    }

    @ApiOperation("Get candidate detail")
    @GetMapping("/{id}")
    public R<CandidateVO> getCandidateDetail(@PathVariable Long id) {
        CandidateVO vo = candidateService.getCandidateDetail(id);
        return R.ok(vo);
    }

    @ApiOperation("List candidates with pagination")
    @GetMapping("/list")
    public R<PageResult<CandidateVO>> listCandidates(CandidateQueryDTO query) {
        PageResult<CandidateVO> result = candidateService.listCandidates(query);
        return R.ok(result);
    }

    @ApiOperation("Add candidate to a job")
    @PostMapping("/{id}/jobs/{jobId}")
    public R<CandidateJobVO> addCandidateToJob(@PathVariable Long id, @PathVariable Long jobId) {
        CandidateJobVO vo = candidateService.addCandidateToJob(id, jobId);
        return R.ok(vo);
    }

    @ApiOperation("Screening operation for a candidate-job pair")
    @PostMapping("/{id}/jobs/{jobId}/screening")
    public R<CandidateJobVO> screening(@PathVariable Long id, @PathVariable Long jobId,
                                        @RequestBody ScreeningDTO dto) {
        CandidateJobVO vo = candidateService.screening(id, jobId, dto);
        return R.ok(vo);
    }

    @ApiOperation("Get decision panel data for a candidate-job pair")
    @GetMapping("/{id}/jobs/{jobId}/decision")
    public R<CandidateJobVO> getDecisionPanel(@PathVariable Long id, @PathVariable Long jobId) {
        CandidateJobVO vo = candidateService.getDecisionPanel(id, jobId);
        return R.ok(vo);
    }

    @ApiOperation("Talent pool global search")
    @GetMapping("/talent-pool")
    public R<PageResult<CandidateVO>> getTalentPool(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) String tags) {
        PageResult<CandidateVO> result = candidateService.getTalentPool(keyword, tags);
        return R.ok(result);
    }
}
