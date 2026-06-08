package com.recruitos.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruitos.agent.dto.*;
import com.recruitos.agent.entity.AgentBehaviorLog;
import com.recruitos.agent.entity.AgentTask;
import com.recruitos.agent.mapper.AgentBehaviorLogMapper;
import com.recruitos.agent.mapper.AgentTaskMapper;
import com.recruitos.agent.mapper.JobPositionReadMapper;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent task service - business logic for agent task management
 */
@Service
public class AgentTaskService {

    @Resource
    private AgentTaskMapper taskMapper;

    @Resource
    private AgentBehaviorLogMapper behaviorLogMapper;

    @Resource
    private JobPositionReadMapper jobPositionReadMapper;

    @Resource
    private SourcingCampaignService sourcingCampaignService;

    /**
     * Create a new agent task
     */
    public AgentTaskVO createTask(AgentTaskCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();

        if (dto.getJobId() != null) {
            String jobStatus = jobPositionReadMapper.selectStatus(dto.getJobId(), tenantId);
            if (!"ACTIVE".equals(jobStatus)) {
                throw new BizException("Agent task requires an ACTIVE job position");
            }
        }

        AgentTask task = new AgentTask();
        task.setTenantId(tenantId);
        task.setTaskType(dto.getTaskType());
        task.setJobId(dto.getJobId());
        task.setJobTitle(dto.getJobTitle());
        task.setAgentAccountId(dto.getAgentAccountId());
        task.setPlatform(dto.getPlatform());
        task.setStatus("PENDING");
        task.setPriority(dto.getPriority() != null ? dto.getPriority() : 2);
        task.setTargetCount(dto.getTargetCount());
        task.setCompletedCount(0);
        task.setFailedCount(0);
        task.setCreatedBy(null); // Will be set by auth context
        taskMapper.insert(task);

        return convertToVO(task, false);
    }

    /**
     * Start a task - change status to RUNNING
     */
    public AgentTaskVO startTask(Long id) {
        Long tenantId = TenantContext.getTenantId();

        AgentTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BizException("Agent task not found");
        }
        if (!task.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if (!"PENDING".equals(task.getStatus()) && !"PAUSED".equals(task.getStatus())) {
            throw new BizException("Task can only be started from PENDING or PAUSED status");
        }

        task.setStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now());
        taskMapper.updateById(task);

        return convertToVO(task, false);
    }

    /**
     * Pause a task
     */
    public AgentTaskVO pauseTask(Long id) {
        Long tenantId = TenantContext.getTenantId();

        AgentTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BizException("Agent task not found");
        }
        if (!task.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if (!"RUNNING".equals(task.getStatus())) {
            throw new BizException("Task can only be paused from RUNNING status");
        }

        task.setStatus("PAUSED");
        taskMapper.updateById(task);
        try {
            sourcingCampaignService.pauseAllRunning();
        } catch (Exception ignored) {
            // best-effort: stop background RPA when user stops agent task
        }

        return convertToVO(task, false);
    }

    /**
     * Resume a paused task
     */
    public AgentTaskVO resumeTask(Long id) {
        Long tenantId = TenantContext.getTenantId();

        AgentTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BizException("Agent task not found");
        }
        if (!task.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if (!"PAUSED".equals(task.getStatus())) {
            throw new BizException("Task can only be resumed from PAUSED status");
        }

        task.setStatus("RUNNING");
        taskMapper.updateById(task);

        return convertToVO(task, false);
    }

    /**
     * Get paginated task list with filters
     */
    public PageResult<AgentTaskVO> getTaskList(AgentTaskQueryDTO query) {
        Long tenantId = TenantContext.getTenantId();

        Page<AgentTask> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<AgentTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentTask::getTenantId, tenantId);

        if (StringUtils.hasText(query.getTaskType())) {
            wrapper.eq(AgentTask::getTaskType, query.getTaskType());
        }
        if (StringUtils.hasText(query.getPlatform())) {
            wrapper.eq(AgentTask::getPlatform, query.getPlatform());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(AgentTask::getStatus, query.getStatus());
        }
        if (query.getAgentAccountId() != null) {
            wrapper.eq(AgentTask::getAgentAccountId, query.getAgentAccountId());
        }
        if (query.getJobId() != null) {
            wrapper.eq(AgentTask::getJobId, query.getJobId());
        }

        wrapper.orderByDesc(AgentTask::getCreatedAt);

        Page<AgentTask> result = taskMapper.selectPage(page, wrapper);

        List<AgentTaskVO> voList = new ArrayList<>();
        for (AgentTask task : result.getRecords()) {
            voList.add(convertToVO(task, false));
        }

        return new PageResult<>(result.getTotal(), voList, query.getPageNum(), query.getPageSize());
    }

    /**
     * Get task detail with recent logs
     */
    public AgentTaskVO getTaskDetail(Long id) {
        Long tenantId = TenantContext.getTenantId();

        AgentTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BizException("Agent task not found");
        }
        if (!task.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        return convertToVO(task, true);
    }

    /**
     * Convert AgentTask entity to AgentTaskVO
     * @param includeLogs whether to load and include recent logs
     */
    private AgentTaskVO convertToVO(AgentTask task, boolean includeLogs) {
        AgentTaskVO vo = new AgentTaskVO();
        vo.setId(task.getId());
        vo.setTenantId(task.getTenantId());
        vo.setTaskType(task.getTaskType());
        vo.setJobId(task.getJobId());
        vo.setJobTitle(task.getJobTitle());
        vo.setAgentAccountId(task.getAgentAccountId());
        vo.setPlatform(task.getPlatform());
        vo.setStatus(task.getStatus());
        vo.setPriority(task.getPriority());
        vo.setTargetCount(task.getTargetCount());
        vo.setCompletedCount(task.getCompletedCount());
        vo.setFailedCount(task.getFailedCount());
        vo.setStartedAt(task.getStartedAt());
        vo.setCompletedAt(task.getCompletedAt());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setCreatedBy(task.getCreatedBy());
        vo.setCreatedAt(task.getCreatedAt());
        vo.setUpdatedAt(task.getUpdatedAt());

        if (includeLogs) {
            LambdaQueryWrapper<AgentBehaviorLog> logWrapper = new LambdaQueryWrapper<>();
            logWrapper.eq(AgentBehaviorLog::getAgentTaskId, task.getId())
                    .orderByDesc(AgentBehaviorLog::getExecutedAt)
                    .last("LIMIT 20");
            List<AgentBehaviorLog> logs = behaviorLogMapper.selectList(logWrapper);

            List<BehaviorLogVO> logVOs = new ArrayList<>();
            for (AgentBehaviorLog log : logs) {
                logVOs.add(convertLogToVO(log));
            }
            vo.setRecentLogs(logVOs);
        }

        return vo;
    }

    /**
     * Convert AgentBehaviorLog entity to BehaviorLogVO
     */
    private BehaviorLogVO convertLogToVO(AgentBehaviorLog log) {
        BehaviorLogVO vo = new BehaviorLogVO();
        vo.setId(log.getId());
        vo.setTenantId(log.getTenantId());
        vo.setAgentTaskId(log.getAgentTaskId());
        vo.setAgentAccountId(log.getAgentAccountId());
        vo.setActionType(log.getActionType());
        vo.setTargetName(log.getTargetName());
        vo.setTargetPlatform(log.getTargetPlatform());
        vo.setActionDetail(log.getActionDetail());
        vo.setIsSuccess(log.getIsSuccess());
        vo.setErrorMessage(log.getErrorMessage());
        vo.setRandomDelay(log.getRandomDelay());
        vo.setExecutedAt(log.getExecutedAt());
        vo.setCreatedAt(log.getCreatedAt());
        return vo;
    }
}
