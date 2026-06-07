package com.recruitos.communication.controller;

import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import com.recruitos.communication.dto.SafetyLogQueryDTO;
import com.recruitos.communication.dto.SafetyLogVO;
import com.recruitos.communication.service.SafetyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * Safety check management controller
 */
@Api(tags = "Safety Check Management")
@RestController
@RequestMapping("/api/communication/safety")
public class SafetyController {

    @Resource
    private SafetyService safetyService;

    @ApiOperation("Get paginated safety log list")
    @GetMapping("/list")
    public R<PageResult<SafetyLogVO>> getSafetyLogList(SafetyLogQueryDTO query) {
        PageResult<SafetyLogVO> result = safetyService.getSafetyLogList(query);
        return R.ok(result);
    }

    @ApiOperation("Get safety statistics")
    @GetMapping("/stats")
    public R<Map<String, Object>> getSafetyStats() {
        Map<String, Object> stats = safetyService.getSafetyStats();
        return R.ok(stats);
    }

    @ApiOperation("Review a message (manual safety review)")
    @PostMapping("/{id}/review")
    public R<SafetyLogVO> reviewMessage(@PathVariable Long id, @RequestParam String action) {
        SafetyLogVO vo = safetyService.reviewMessage(id, action);
        return R.ok(vo);
    }
}
