package com.recruitos.agent.controller;

import com.recruitos.agent.dto.RecruitmentChannelCreateDTO;
import com.recruitos.agent.dto.RecruitmentChannelQueryDTO;
import com.recruitos.agent.dto.RecruitmentChannelVO;
import com.recruitos.agent.service.RecruitmentChannelService;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Api(tags = "Recruitment Channel Management")
@RestController
@RequestMapping("/api/agent/channel")
public class RecruitmentChannelController {

    @Resource
    private RecruitmentChannelService channelService;

    @ApiOperation("List channels with pagination")
    @GetMapping("/list")
    public R<PageResult<RecruitmentChannelVO>> list(RecruitmentChannelQueryDTO query) {
        return R.ok(channelService.listChannels(query));
    }

    @ApiOperation("List active agent-enabled channels")
    @GetMapping("/agent-options")
    public R<List<RecruitmentChannelVO>> agentOptions() {
        return R.ok(channelService.listActiveAgentChannels());
    }

    @ApiOperation("Channel statistics")
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        return R.ok(channelService.getStats());
    }

    @ApiOperation("Get channel detail")
    @GetMapping("/{id}")
    public R<RecruitmentChannelVO> detail(@PathVariable Long id) {
        return R.ok(channelService.getChannelDetail(id));
    }

    @ApiOperation("Create channel")
    @PostMapping
    public R<RecruitmentChannelVO> create(@RequestBody RecruitmentChannelCreateDTO dto) {
        return R.ok(channelService.createChannel(dto));
    }

    @ApiOperation("Update channel")
    @PutMapping("/{id}")
    public R<RecruitmentChannelVO> update(@PathVariable Long id, @RequestBody RecruitmentChannelCreateDTO dto) {
        return R.ok(channelService.updateChannel(id, dto));
    }

    @ApiOperation("Update channel status")
    @PutMapping("/{id}/status")
    public R<RecruitmentChannelVO> updateStatus(@PathVariable Long id, @RequestParam String status) {
        RecruitmentChannelCreateDTO dto = new RecruitmentChannelCreateDTO();
        dto.setStatus(status);
        return R.ok(channelService.updateChannel(id, dto));
    }

    @ApiOperation("Delete channel")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        channelService.deleteChannel(id);
        return R.ok();
    }
}
