package com.recruitos.onboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.onboard.dto.*;
import com.recruitos.onboard.entity.Onboard;
import com.recruitos.onboard.entity.OnboardTask;
import com.recruitos.onboard.mapper.OnboardMapper;
import com.recruitos.onboard.mapper.OnboardTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * Onboard service - core business logic for onboard management
 *
 * Status flow:
 * PENDING -> CONFIRMED -> COMPLETED
 *        -> CANCELLED
 */
@Service
public class OnboardService {

    @Resource
    private OnboardMapper onboardMapper;

    @Resource
    private OnboardTaskMapper onboardTaskMapper;

    /**
     * Create an onboard record with default tasks
     */
    @Transactional
    public OnboardVO createOnboard(OnboardCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BizException("User not authenticated");
        }

        // Create onboard record
        Onboard onboard = new Onboard();
        onboard.setTenantId(tenantId);
        onboard.setCandidateId(dto.getCandidateId());
        onboard.setCandidateName(dto.getCandidateName());
        onboard.setJobId(dto.getJobId());
        onboard.setJobTitle(dto.getJobTitle());
        onboard.setOfferId(dto.getOfferId());
        onboard.setOnboardDate(dto.getOnboardDate());
        onboard.setOnboardStatus("PENDING");
        onboard.setHrId(dto.getHrId());
        onboard.setHrName(dto.getHrName());
        onboard.setRemark(dto.getRemark());
        onboard.setCreatedBy(tenantId);

        onboardMapper.insert(onboard);

        // Create default tasks if provided
        if (dto.getDefaultTasks() != null && !dto.getDefaultTasks().isEmpty()) {
            for (OnboardTaskCreateDTO taskDTO : dto.getDefaultTasks()) {
                createOnboardTaskInternal(tenantId, onboard.getId(), taskDTO);
            }
        }

        return convertToVO(onboard);
    }

    /**
     * Get paginated onboard list with filters
     */
    public PageResult<OnboardVO> getOnboardList(OnboardQueryDTO query) {
        Long tenantId = TenantContext.getTenantId();

        Page<Onboard> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<Onboard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Onboard::getTenantId, tenantId);

        if (query.getCandidateId() != null) {
            wrapper.eq(Onboard::getCandidateId, query.getCandidateId());
        }
        if (query.getJobId() != null) {
            wrapper.eq(Onboard::getJobId, query.getJobId());
        }
        if (StringUtils.hasText(query.getOnboardStatus())) {
            wrapper.eq(Onboard::getOnboardStatus, query.getOnboardStatus());
        }
        if (query.getHrId() != null) {
            wrapper.eq(Onboard::getHrId, query.getHrId());
        }
        if (StringUtils.hasText(query.getCandidateName())) {
            wrapper.like(Onboard::getCandidateName, query.getCandidateName());
        }

        wrapper.orderByDesc(Onboard::getCreatedAt);

        Page<Onboard> result = onboardMapper.selectPage(page, wrapper);

        List<OnboardVO> voList = new ArrayList<>();
        for (Onboard onboard : result.getRecords()) {
            voList.add(convertToVO(onboard));
        }

        return new PageResult<>(result.getTotal(), voList, query.getPageNum(), query.getPageSize());
    }

    /**
     * Get onboard detail with tasks
     */
    public OnboardVO getOnboardDetail(Long id) {
        Long tenantId = TenantContext.getTenantId();

        Onboard onboard = onboardMapper.selectById(id);
        if (onboard == null) {
            throw new BizException("Onboard record not found");
        }
        if (!onboard.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        return convertToVO(onboard);
    }

    /**
     * Update onboard status
     */
    @Transactional
    public OnboardVO updateOnboardStatus(Long id, String status) {
        Long tenantId = TenantContext.getTenantId();

        Onboard onboard = onboardMapper.selectById(id);
        if (onboard == null) {
            throw new BizException("Onboard record not found");
        }
        if (!onboard.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        // Validate status transition
        String currentStatus = onboard.getOnboardStatus();
        if (!isValidStatusTransition(currentStatus, status)) {
            throw new BizException("Invalid status transition from " + currentStatus + " to " + status);
        }

        onboard.setOnboardStatus(status);
        onboardMapper.updateById(onboard);

        return convertToVO(onboard);
    }

    /**
     * Create a task for an onboard record
     */
    @Transactional
    public OnboardTaskVO createTask(OnboardTaskCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BizException("User not authenticated");
        }

        // Verify onboard exists and belongs to tenant
        Onboard onboard = onboardMapper.selectById(dto.getOnboardId());
        if (onboard == null) {
            throw new BizException("Onboard record not found");
        }
        if (!onboard.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        OnboardTask task = createOnboardTaskInternal(tenantId, dto.getOnboardId(), dto);

        return convertTaskToVO(task);
    }

    /**
     * Update task status
     */
    @Transactional
    public OnboardTaskVO updateTaskStatus(Long taskId, String status) {
        Long tenantId = TenantContext.getTenantId();

        OnboardTask task = onboardTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException("Task not found");
        }
        if (!task.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        task.setTaskStatus(status);
        onboardTaskMapper.updateById(task);

        return convertTaskToVO(task);
    }

    /**
     * Get tasks for an onboard record
     */
    public List<OnboardTaskVO> getTaskList(Long onboardId) {
        Long tenantId = TenantContext.getTenantId();

        // Verify onboard exists and belongs to tenant
        Onboard onboard = onboardMapper.selectById(onboardId);
        if (onboard == null) {
            throw new BizException("Onboard record not found");
        }
        if (!onboard.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        LambdaQueryWrapper<OnboardTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OnboardTask::getOnboardId, onboardId);
        wrapper.orderByAsc(OnboardTask::getCreatedAt);

        List<OnboardTask> tasks = onboardTaskMapper.selectList(wrapper);

        List<OnboardTaskVO> voList = new ArrayList<>();
        for (OnboardTask task : tasks) {
            voList.add(convertTaskToVO(task));
        }

        return voList;
    }

    /**
     * Internal method to create an onboard task
     */
    private OnboardTask createOnboardTaskInternal(Long tenantId, Long onboardId, OnboardTaskCreateDTO dto) {
        OnboardTask task = new OnboardTask();
        task.setTenantId(tenantId);
        task.setOnboardId(onboardId);
        task.setTaskName(dto.getTaskName());
        task.setTaskType(dto.getTaskType());
        task.setAssigneeId(dto.getAssigneeId());
        task.setAssigneeName(dto.getAssigneeName());
        task.setDueDate(dto.getDueDate());
        task.setTaskStatus("PENDING");
        task.setRemark(dto.getRemark());

        onboardTaskMapper.insert(task);

        return task;
    }

    /**
     * Validate status transition
     */
    private boolean isValidStatusTransition(String current, String next) {
        switch (current) {
            case "PENDING":
                return "CONFIRMED".equals(next) || "CANCELLED".equals(next);
            case "CONFIRMED":
                return "COMPLETED".equals(next) || "CANCELLED".equals(next);
            case "CANCELLED":
            case "COMPLETED":
                return false;
            default:
                return false;
        }
    }

    /**
     * Convert Onboard entity to OnboardVO with tasks
     */
    private OnboardVO convertToVO(Onboard onboard) {
        OnboardVO vo = new OnboardVO();
        vo.setId(onboard.getId());
        vo.setTenantId(onboard.getTenantId());
        vo.setCandidateId(onboard.getCandidateId());
        vo.setCandidateName(onboard.getCandidateName());
        vo.setJobId(onboard.getJobId());
        vo.setJobTitle(onboard.getJobTitle());
        vo.setOfferId(onboard.getOfferId());
        vo.setOnboardDate(onboard.getOnboardDate());
        vo.setOnboardStatus(onboard.getOnboardStatus());
        vo.setHrId(onboard.getHrId());
        vo.setHrName(onboard.getHrName());
        vo.setRemark(onboard.getRemark());
        vo.setCreatedBy(onboard.getCreatedBy());
        vo.setCreatedAt(onboard.getCreatedAt());
        vo.setUpdatedAt(onboard.getUpdatedAt());

        // Load tasks
        LambdaQueryWrapper<OnboardTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(OnboardTask::getOnboardId, onboard.getId());
        taskWrapper.orderByAsc(OnboardTask::getCreatedAt);
        List<OnboardTask> tasks = onboardTaskMapper.selectList(taskWrapper);

        List<OnboardTaskVO> taskVOs = new ArrayList<>();
        for (OnboardTask task : tasks) {
            taskVOs.add(convertTaskToVO(task));
        }
        vo.setTasks(taskVOs);

        return vo;
    }

    /**
     * Convert OnboardTask entity to OnboardTaskVO
     */
    private OnboardTaskVO convertTaskToVO(OnboardTask task) {
        OnboardTaskVO vo = new OnboardTaskVO();
        vo.setId(task.getId());
        vo.setTenantId(task.getTenantId());
        vo.setOnboardId(task.getOnboardId());
        vo.setTaskName(task.getTaskName());
        vo.setTaskType(task.getTaskType());
        vo.setAssigneeId(task.getAssigneeId());
        vo.setAssigneeName(task.getAssigneeName());
        vo.setDueDate(task.getDueDate());
        vo.setTaskStatus(task.getTaskStatus());
        vo.setRemark(task.getRemark());
        vo.setCreatedAt(task.getCreatedAt());
        vo.setUpdatedAt(task.getUpdatedAt());
        return vo;
    }
}
