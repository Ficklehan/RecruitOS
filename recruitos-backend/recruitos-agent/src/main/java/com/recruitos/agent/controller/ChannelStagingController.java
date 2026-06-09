package com.recruitos.agent.controller;

import com.recruitos.agent.dto.ChannelStagingQueryDTO;
import com.recruitos.agent.dto.ChannelStagingVO;
import com.recruitos.agent.service.ChannelStagingService;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Api(tags = "渠道暂存库")
@RestController
@RequestMapping("/api/agent/channel-staging")
public class ChannelStagingController {

    @Resource
    private ChannelStagingService channelStagingService;

    @ApiOperation("暂存列表")
    @GetMapping
    public R<PageResult<ChannelStagingVO>> list(ChannelStagingQueryDTO query) {
        return R.ok(channelStagingService.list(query));
    }

    @ApiOperation("暂存详情")
    @GetMapping("/{id}")
    public R<ChannelStagingVO> get(@PathVariable Long id) {
        return R.ok(channelStagingService.getById(id));
    }

    @ApiOperation("更新提取字段")
    @PutMapping("/{id}/fields")
    public R<ChannelStagingVO> updateFields(@PathVariable Long id, @RequestBody Map<String, Object> fields) {
        return R.ok(channelStagingService.updateExtractedFields(id, fields));
    }

    @ApiOperation("AI 问答（MVP 规则提取）")
    @PostMapping("/{id}/ask")
    public R<Map<String, Object>> ask(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return R.ok(channelStagingService.askAi(id, body.get("question")));
    }

    @ApiOperation("批量打招呼")
    @PostMapping("/batch/greet")
    public R<Map<String, Object>> batchGreet(@RequestBody Map<String, List<Long>> body) {
        return R.ok(channelStagingService.batchGreet(body.get("ids")));
    }

    @ApiOperation("批量确认入库")
    @PostMapping("/batch/import")
    public R<Map<String, Object>> batchImport(@RequestBody Map<String, List<Long>> body) {
        return R.ok(channelStagingService.batchImport(body.get("ids")));
    }

    @ApiOperation("批量标记不合适")
    @PostMapping("/batch/reject")
    public R<Map<String, Object>> batchReject(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) body.get("ids");
        String reason = body.get("reason") != null ? body.get("reason").toString() : null;
        return R.ok(channelStagingService.batchReject(ids, reason));
    }
}
