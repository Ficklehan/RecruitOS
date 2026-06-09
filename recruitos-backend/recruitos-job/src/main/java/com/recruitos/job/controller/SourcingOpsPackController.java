package com.recruitos.job.controller;

import com.recruitos.common.result.R;
import com.recruitos.job.dto.SourcingOpsPackVO;
import com.recruitos.job.service.SourcingOpsPackService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Api(tags = "Sourcing Ops Pack")
@RestController
@RequestMapping("/api/job/{jobId}/ops-pack")
public class SourcingOpsPackController {

    @Resource
    private SourcingOpsPackService sourcingOpsPackService;

    @ApiOperation("从 JD 标签生成运营包草案")
    @PostMapping("/generate")
    public R<SourcingOpsPackVO> generate(@PathVariable Long jobId) {
        return R.ok(sourcingOpsPackService.generateDraft(jobId));
    }

    @ApiOperation("获取当前生效运营包")
    @GetMapping("/active")
    public R<SourcingOpsPackVO> getActive(@PathVariable Long jobId) {
        return R.ok(sourcingOpsPackService.getActive(jobId));
    }

    @ApiOperation("运营包版本列表")
    @GetMapping("/versions")
    public R<List<SourcingOpsPackVO>> listVersions(@PathVariable Long jobId) {
        return R.ok(sourcingOpsPackService.listVersions(jobId));
    }

    @ApiOperation("运营包详情")
    @GetMapping("/{packId}")
    public R<SourcingOpsPackVO> getById(@PathVariable Long jobId, @PathVariable Long packId) {
        return R.ok(sourcingOpsPackService.getById(jobId, packId));
    }

    @ApiOperation("更新草案")
    @PutMapping("/{packId}")
    public R<SourcingOpsPackVO> update(@PathVariable Long jobId, @PathVariable Long packId,
                                       @RequestBody Map<String, Object> packBody) {
        return R.ok(sourcingOpsPackService.updateDraft(jobId, packId, packBody));
    }

    @ApiOperation("HR 确认运营包生效")
    @PostMapping("/{packId}/confirm")
    public R<SourcingOpsPackVO> confirm(@PathVariable Long jobId, @PathVariable Long packId) {
        return R.ok(sourcingOpsPackService.confirm(jobId, packId));
    }

    @ApiOperation("采纳进化建议并发布新运营包")
    @PostMapping("/apply-evolution")
    public R<SourcingOpsPackVO> applyEvolution(@PathVariable Long jobId,
                                               @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        Map<String, Object> pack = (Map<String, Object>) body.get("pack");
        Long proposalId = body.get("proposalId") instanceof Number
                ? ((Number) body.get("proposalId")).longValue() : null;
        return R.ok(sourcingOpsPackService.applyFromEvolution(jobId, pack, proposalId));
    }
}
