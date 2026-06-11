package com.recruitos.onboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruitos.common.evolution.ModuleEvolutionEmitter;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.onboard.dto.*;
import com.recruitos.onboard.entity.Onboard;
import com.recruitos.onboard.entity.OnboardTask;
import com.recruitos.onboard.mapper.HeadcountWriteMapper;
import com.recruitos.onboard.mapper.OnboardMapper;
import com.recruitos.onboard.mapper.OnboardTaskMapper;
import com.recruitos.onboard.mapper.ReferralOnboardSyncMapper;
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

    @Resource
    private HeadcountWriteMapper headcountWriteMapper;

    @Resource
    private ModuleEvolutionEmitter moduleEvolutionEmitter;

    @Resource
    private ReferralOnboardSyncMapper referralOnboardSyncMapper;

    private static final double DEFAULT_REFERRAL_REWARD = 3000.0;

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
     * 试用期考核结果回流（L6 进化信号，模拟 HRone Webhook）
     */
    @Transactional
    public OnboardVO submitProbationFeedback(Long id, ProbationFeedbackDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Onboard onboard = onboardMapper.selectById(id);
        if (onboard == null || !onboard.getTenantId().equals(tenantId)) {
            throw new BizException("Onboard record not found");
        }
        if (dto == null || dto.getPassed() == null) {
            throw new BizException("请提供试用期是否通过");
        }
        String status = onboard.getOnboardStatus();
        if ("COMPLETED".equals(status)) {
            return convertToVO(onboard);
        }
        if (!"CONFIRMED".equals(status)) {
            throw new BizException("仅已确认入职的记录可提交试用期反馈");
        }

        Long jobId = onboard.getJobId();
        if (jobId == null && onboard.getOfferId() != null) {
            jobId = headcountWriteMapper.selectJobIdByOffer(onboard.getOfferId(), tenantId);
        }
        if (jobId == null) {
            throw new BizException("无法解析岗位 ID，请补全入职记录的 jobId");
        }

        boolean passed = Boolean.TRUE.equals(dto.getPassed());
        moduleEvolutionEmitter.emitProbationResult(
                jobId, onboard.getCandidateId(), passed, dto.getOverallScore());

        if (StringUtils.hasText(dto.getComment())) {
            String remark = onboard.getRemark() != null ? onboard.getRemark() + "\n" : "";
            onboard.setRemark(remark + "[试用期] " + dto.getComment());
        }
        if (passed && "CONFIRMED".equals(status)) {
            onboard.setOnboardStatus("COMPLETED");
        }
        onboardMapper.updateById(onboard);
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

        if ("CONFIRMED".equals(status)) {
            syncReferralOnboard(tenantId, onboard.getCandidateId());
        }

        if ("COMPLETED".equals(status)) {
            completeHeadcountWriteback(tenantId, onboard);
        }

        return convertToVO(onboard);
    }

    private void syncReferralOnboard(Long tenantId, Long candidateId) {
        if (candidateId == null) {
            return;
        }
        try {
            referralOnboardSyncMapper.markHired(tenantId, candidateId, DEFAULT_REFERRAL_REWARD);
            referralOnboardSyncMapper.createPendingReward(tenantId, candidateId, DEFAULT_REFERRAL_REWARD);
        } catch (Exception ignored) {
            /* best-effort */
        }
    }

    private void completeHeadcountWriteback(Long tenantId, Onboard onboard) {
        if (onboard.getJobId() != null) {
            headcountWriteMapper.incrementJobFilled(onboard.getJobId(), tenantId);
            Long demandId = headcountWriteMapper.selectDemandId(onboard.getJobId(), tenantId);
            if (demandId != null) {
                headcountWriteMapper.incrementDemandFilled(demandId, tenantId);
            }
            if (onboard.getCandidateId() != null) {
                headcountWriteMapper.markCandidateHired(tenantId, onboard.getCandidateId(), onboard.getJobId());
                headcountWriteMapper.markCandidateOnboard(onboard.getCandidateId(), tenantId);
            }
        }
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

        task.setTaskStatus(normalizeTaskStatus(status));
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
    private String normalizeTaskStatus(String status) {
        if ("COMPLETED".equals(status)) {
            return "DONE";
        }
        return status;
    }

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
        String taskStatus = task.getTaskStatus();
        vo.setTaskStatus("DONE".equals(taskStatus) ? "COMPLETED" : taskStatus);
        vo.setRemark(task.getRemark());
        vo.setCreatedAt(task.getCreatedAt());
        vo.setUpdatedAt(task.getUpdatedAt());
        return vo;
    }
}
