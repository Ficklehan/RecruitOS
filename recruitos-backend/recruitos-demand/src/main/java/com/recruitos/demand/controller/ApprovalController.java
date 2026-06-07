package com.recruitos.demand.controller;

import com.recruitos.common.auth.CurrentUser;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import com.recruitos.demand.dto.DemandVO;
import com.recruitos.demand.entity.ApprovalInstance;
import com.recruitos.demand.entity.ApprovalRecord;
import com.recruitos.demand.service.DemandService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Approval management controller
 */
@Api(tags = "Approval Management")
@RestController
@RequestMapping("/api/approval")
public class ApprovalController {

    @Resource
    private DemandService demandService;

    @ApiOperation("Get my approval list (demands pending my approval)")
    @GetMapping("/my")
    public R<PageResult<DemandVO>> getMyApprovals(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = CurrentUser.getCurrentUserId();
        PageResult<DemandVO> result = demandService.getMyApprovals(userId, pageNum, pageSize);
        return R.ok(result);
    }

    @ApiOperation("Get approval detail with records")
    @GetMapping("/{instanceId}")
    public R<Map<String, Object>> getApprovalDetail(@PathVariable Long instanceId) {
        ApprovalInstance instance = demandService.getApprovalDetail(instanceId);
        List<ApprovalRecord> records = demandService.getApprovalRecords(instanceId);

        Map<String, Object> result = new HashMap<>();
        result.put("instance", instance);
        result.put("records", records);

        return R.ok(result);
    }

    @ApiOperation("Approve a demand")
    @PostMapping("/{instanceId}/approve")
    public R<DemandVO> approve(@PathVariable Long instanceId,
                               @RequestParam(required = false) String comment) {
        DemandVO vo = demandService.approveDemand(instanceId, "APPROVE", comment);
        return R.ok(vo);
    }

    @ApiOperation("Reject a demand")
    @PostMapping("/{instanceId}/reject")
    public R<DemandVO> reject(@PathVariable Long instanceId,
                              @RequestParam(required = false) String comment) {
        DemandVO vo = demandService.approveDemand(instanceId, "REJECT", comment);
        return R.ok(vo);
    }
}
