package com.recruitos.agent.controller;

import com.recruitos.agent.dto.CampaignCandidateTraceVO;
import com.recruitos.agent.dto.WorkflowCreateDTO;
import com.recruitos.agent.dto.WorkflowVO;
import com.recruitos.agent.service.CampaignOrchestratorService;
import com.recruitos.agent.service.SourcingCampaignService;
import com.recruitos.common.result.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "岗位寻源工作流")
@RestController
@RequestMapping("/api/agent/workflow")
public class WorkflowController {

    @Resource
    private SourcingCampaignService sourcingCampaignService;
    @Resource
    private CampaignOrchestratorService orchestratorService;

    @ApiOperation("创建并启动寻源活动")
    @PostMapping
    public R<WorkflowVO> create(@RequestBody WorkflowCreateDTO dto) {
        return R.ok(sourcingCampaignService.createAndStart(dto));
    }

    @ApiOperation("活动列表")
    @GetMapping("/list")
    public R<List<WorkflowVO>> list(@RequestParam(required = false) Long jobId) {
        return R.ok(sourcingCampaignService.listWorkflows(jobId));
    }

    @ApiOperation("活动详情")
    @GetMapping("/{id}")
    public R<WorkflowVO> detail(@PathVariable Long id) {
        return R.ok(sourcingCampaignService.getWorkflowDetail(id));
    }

    @ApiOperation("活动统计")
    @GetMapping("/{id}/stats")
    public R<WorkflowVO.WorkflowStatsVO> stats(@PathVariable Long id) {
        return R.ok(sourcingCampaignService.getStats(id));
    }

    @ApiOperation("暂停活动")
    @PostMapping("/{id}/pause")
    public R<WorkflowVO> pause(@PathVariable Long id) {
        return R.ok(sourcingCampaignService.pause(id));
    }

    @ApiOperation("暂停全部运行中的寻源活动")
    @PostMapping("/pause-all")
    public R<Map<String, Object>> pauseAll() {
        int count = sourcingCampaignService.pauseAllRunning();
        Map<String, Object> body = new HashMap<>();
        body.put("pausedCount", count);
        return R.ok(body);
    }

    @ApiOperation("恢复活动")
    @PostMapping("/{id}/resume")
    public R<WorkflowVO> resume(@PathVariable Long id) {
        return R.ok(sourcingCampaignService.resume(id));
    }

    @ApiOperation("候选人轨迹")
    @GetMapping("/{id}/candidates")
    public R<List<CampaignCandidateTraceVO>> candidates(@PathVariable Long id,
                                                       @RequestParam(required = false) String status) {
        return R.ok(sourcingCampaignService.listTraces(id, status));
    }

    @ApiOperation("确认发布岗位到平台")
    @PostMapping("/{id}/confirm-publish")
    public R<Map<String, Object>> confirmPublish(@PathVariable Long id) {
        orchestratorService.confirmPublish(id);
        Map<String, Object> body = new HashMap<>();
        body.put("ok", true);
        return R.ok(body);
    }

    @ApiOperation("确认发送打招呼")
    @PostMapping("/trace/{traceId}/confirm-greet")
    public R<Map<String, Object>> confirmGreet(@PathVariable Long traceId) {
        orchestratorService.confirmGreet(traceId);
        Map<String, Object> body = new HashMap<>();
        body.put("ok", true);
        return R.ok(body);
    }

    @ApiOperation("确认简历入库")
    @PostMapping("/trace/{traceId}/confirm-import")
    public R<Map<String, Object>> confirmImport(@PathVariable Long traceId) {
        orchestratorService.confirmImport(traceId);
        Map<String, Object> body = new HashMap<>();
        body.put("ok", true);
        return R.ok(body);
    }
}
