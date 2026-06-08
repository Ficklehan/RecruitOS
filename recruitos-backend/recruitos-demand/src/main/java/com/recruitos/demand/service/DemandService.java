package com.recruitos.demand.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruitos.common.auth.CurrentUser;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.demand.dto.*;
import com.recruitos.demand.entity.ApprovalInstance;
import com.recruitos.demand.entity.ApprovalRecord;
import com.recruitos.demand.entity.RecruitDemand;
import com.recruitos.demand.mapper.ApprovalInstanceMapper;
import com.recruitos.demand.mapper.ApprovalRecordMapper;
import com.recruitos.demand.mapper.JobDraftMapper;
import com.recruitos.demand.mapper.RecruitDemandMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Demand service - core business logic for recruitment demand management
 */
@Service
public class DemandService {

    @Resource
    private RecruitDemandMapper recruitDemandMapper;

    @Resource
    private ApprovalInstanceMapper approvalInstanceMapper;

    @Resource
    private ApprovalRecordMapper approvalRecordMapper;

    @Resource
    private JobDraftMapper jobDraftMapper;

    /** Sequence counter for demand number generation (in-memory, resets on restart) */
    private final AtomicLong demandSeqCounter = new AtomicLong(0);

    private final AtomicLong jobSeqCounter = new AtomicLong(0);

    /** Approval node names */
    private static final String NODE_HRBP = "HRBP审核";
    private static final String NODE_HR_MANAGER = "HR负责人审批";
    private static final String NODE_HRD = "集团HRD审批";

    /**
     * Create a new recruitment demand
     */
    @Transactional
    public DemandVO createDemand(DemandCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = CurrentUser.getCurrentUserId();
        if (tenantId == null || userId == null) {
            throw new BizException("User not authenticated");
        }

        RecruitDemand demand = new RecruitDemand();
        demand.setTenantId(tenantId);
        demand.setDemandNo(generateDemandNo());
        demand.setTitle(dto.getTitle());
        demand.setOrgId(dto.getOrgId());
        demand.setHeadCount(dto.getHeadCount());
        demand.setJobLevel(dto.getJobLevel());
        demand.setSalaryMin(dto.getSalaryMin());
        demand.setSalaryMax(dto.getSalaryMax());
        demand.setUrgency(dto.getUrgency() != null ? dto.getUrgency() : "NORMAL");
        demand.setExpectedOnboardDate(dto.getExpectedOnboardDate());
        demand.setReason(dto.getReason());
        demand.setJobDuty(dto.getJobDuty());
        demand.setJobRequirement(dto.getJobRequirement());
        demand.setWorkLocations(dto.getWorkLocations());
        demand.setReporterId(dto.getReporterId());
        demand.setInitialInterviewerIds(dto.getInitialInterviewerIds());
        demand.setFinalInterviewerIds(dto.getFinalInterviewerIds());
        demand.setStatus("DRAFT");
        demand.setApprovedHeadCount(0);
        demand.setFilledCount(0);
        demand.setCreatedBy(userId);

        recruitDemandMapper.insert(demand);

        return convertToVO(demand);
    }

    /**
     * Update a demand (only DRAFT or REJECTED)
     */
    @Transactional
    public DemandVO updateDemand(Long demandId, DemandCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();

        RecruitDemand demand = recruitDemandMapper.selectById(demandId);
        if (demand == null) {
            throw new BizException("Demand not found");
        }
        if (!demand.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if (!"DRAFT".equals(demand.getStatus()) && !"REJECTED".equals(demand.getStatus())) {
            throw new BizException("Only DRAFT or REJECTED demands can be updated");
        }

        if (dto.getTitle() != null) demand.setTitle(dto.getTitle());
        if (dto.getOrgId() != null) demand.setOrgId(dto.getOrgId());
        if (dto.getHeadCount() != null) demand.setHeadCount(dto.getHeadCount());
        if (dto.getJobLevel() != null) demand.setJobLevel(dto.getJobLevel());
        if (dto.getSalaryMin() != null) demand.setSalaryMin(dto.getSalaryMin());
        if (dto.getSalaryMax() != null) demand.setSalaryMax(dto.getSalaryMax());
        if (dto.getUrgency() != null) demand.setUrgency(dto.getUrgency());
        if (dto.getExpectedOnboardDate() != null) demand.setExpectedOnboardDate(dto.getExpectedOnboardDate());
        if (dto.getReason() != null) demand.setReason(dto.getReason());
        if (dto.getJobDuty() != null) demand.setJobDuty(dto.getJobDuty());
        if (dto.getJobRequirement() != null) demand.setJobRequirement(dto.getJobRequirement());
        if (dto.getWorkLocations() != null) demand.setWorkLocations(dto.getWorkLocations());
        if (dto.getReporterId() != null) demand.setReporterId(dto.getReporterId());
        if (dto.getInitialInterviewerIds() != null) demand.setInitialInterviewerIds(dto.getInitialInterviewerIds());
        if (dto.getFinalInterviewerIds() != null) demand.setFinalInterviewerIds(dto.getFinalInterviewerIds());

        demand.setStatus("DRAFT"); // Reset to DRAFT on edit
        demand.setRejectReason(null);
        recruitDemandMapper.updateById(demand);

        return convertToVO(demand);
    }

    /**
     * Submit demand for approval
     */
    @Transactional
    public DemandVO submitDemand(Long demandId) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = CurrentUser.getCurrentUserId();

        RecruitDemand demand = recruitDemandMapper.selectById(demandId);
        if (demand == null) {
            throw new BizException("Demand not found");
        }
        if (!demand.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if (!"DRAFT".equals(demand.getStatus()) && !"REJECTED".equals(demand.getStatus())) {
            throw new BizException("Only DRAFT or REJECTED demands can be submitted");
        }

        // Update demand status to PENDING
        demand.setStatus("PENDING");
        demand.setRejectReason(null);
        recruitDemandMapper.updateById(demand);

        // Create approval instance
        ApprovalInstance instance = new ApprovalInstance();
        instance.setTenantId(tenantId);
        instance.setBizType("DEMAND");
        instance.setBizId(demandId);
        instance.setStatus("RUNNING");
        instance.setCurrentNode(NODE_HRBP);
        instance.setCurrentApproverId(null); // Will be assigned by HRBP
        approvalInstanceMapper.insert(instance);

        // Create first approval record (submission)
        ApprovalRecord record = new ApprovalRecord();
        record.setInstanceId(instance.getId());
        record.setNodeName("提交审批");
        record.setApproverId(userId);
        record.setAction("SUBMIT");
        record.setComment("提交招聘需求审批");
        record.setCreatedAt(LocalDateTime.now());
        approvalRecordMapper.insert(record);

        return convertToVO(demand);
    }

    /**
     * Approve or reject a demand
     */
    @Transactional
    public DemandVO approveDemand(Long instanceId, String action, String comment) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = CurrentUser.getCurrentUserId();

        ApprovalInstance instance = approvalInstanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new BizException("Approval instance not found");
        }
        if (!instance.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if (!"RUNNING".equals(instance.getStatus())) {
            throw new BizException("Approval instance is not in RUNNING status");
        }

        RecruitDemand demand = recruitDemandMapper.selectById(instance.getBizId());
        if (demand == null) {
            throw new BizException("Associated demand not found");
        }

        // Record approval action
        ApprovalRecord record = new ApprovalRecord();
        record.setInstanceId(instanceId);
        record.setNodeName(instance.getCurrentNode());
        record.setApproverId(userId);
        record.setAction(action);
        record.setComment(comment);
        record.setCreatedAt(LocalDateTime.now());
        approvalRecordMapper.insert(record);

        if ("APPROVE".equals(action)) {
            handleApprove(instance, demand);
        } else if ("REJECT".equals(action)) {
            handleReject(instance, demand, comment);
        } else {
            throw new BizException("Invalid action: " + action);
        }

        return convertToVO(demand);
    }

    /**
     * Handle approval flow - advance to next node or complete
     */
    private void handleApprove(ApprovalInstance instance, RecruitDemand demand) {
        String currentNode = instance.getCurrentNode();

        if (NODE_HRBP.equals(currentNode)) {
            // After HRBP, check if HRD approval is needed (Director level and above)
            boolean needHrdApproval = isDirectorLevelOrAbove(demand.getJobLevel());

            if (needHrdApproval) {
                // Move to HR Manager first
                instance.setCurrentNode(NODE_HR_MANAGER);
                instance.setCurrentApproverId(null);
                approvalInstanceMapper.updateById(instance);
            } else {
                // For non-director levels: HRBP -> HR Manager -> Approved
                // Check if we should move to HR Manager
                instance.setCurrentNode(NODE_HR_MANAGER);
                instance.setCurrentApproverId(null);
                approvalInstanceMapper.updateById(instance);
            }
        } else if (NODE_HR_MANAGER.equals(currentNode)) {
            boolean needHrdApproval = isDirectorLevelOrAbove(demand.getJobLevel());

            if (needHrdApproval) {
                // Move to HRD
                instance.setCurrentNode(NODE_HRD);
                instance.setCurrentApproverId(null);
                approvalInstanceMapper.updateById(instance);
            } else {
                // Final approval for non-director levels
                completeApproval(instance, demand);
            }
        } else if (NODE_HRD.equals(currentNode)) {
            // HRD is the final approver for director+ levels
            completeApproval(instance, demand);
        }
    }

    /**
     * Complete the approval process
     */
    private void completeApproval(ApprovalInstance instance, RecruitDemand demand) {
        instance.setStatus("APPROVED");
        instance.setCurrentNode(null);
        instance.setCurrentApproverId(null);
        approvalInstanceMapper.updateById(instance);

        demand.setStatus("JOB_CREATED");
        demand.setApprovedHeadCount(demand.getHeadCount());
        recruitDemandMapper.updateById(demand);

        createJobDraftFromDemand(demand);
    }

    private void createJobDraftFromDemand(RecruitDemand demand) {
        StringBuilder jd = new StringBuilder();
        if (StringUtils.hasText(demand.getJobDuty())) {
            jd.append("岗位职责：\n").append(demand.getJobDuty()).append("\n\n");
        }
        if (StringUtils.hasText(demand.getJobRequirement())) {
            jd.append("任职要求：\n").append(demand.getJobRequirement());
        }
        String jobNo = "JOB-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%04d", jobSeqCounter.incrementAndGet() % 10000);
        jobDraftMapper.insertDraft(
                demand.getTenantId(),
                demand.getId(),
                jobNo,
                demand.getTitle(),
                jd.toString(),
                demand.getHeadCount()
        );
    }

    /**
     * Handle rejection
     */
    private void handleReject(ApprovalInstance instance, RecruitDemand demand, String comment) {
        instance.setStatus("REJECTED");
        instance.setCurrentNode(null);
        instance.setCurrentApproverId(null);
        approvalInstanceMapper.updateById(instance);

        demand.setStatus("REJECTED");
        demand.setRejectReason(comment);
        recruitDemandMapper.updateById(demand);
    }

    /**
     * Check if job level is Director (P8) or above
     */
    private boolean isDirectorLevelOrAbove(String jobLevel) {
        if (jobLevel == null) {
            return false;
        }
        // Extract numeric level from formats like P8, P9, P10, M4, M5, etc.
        String level = jobLevel.replaceAll("[^0-9]", "");
        try {
            int numericLevel = Integer.parseInt(level);
            if (jobLevel.startsWith("P") && numericLevel >= 8) {
                return true;
            }
            if (jobLevel.startsWith("M") && numericLevel >= 4) {
                return true;
            }
        } catch (NumberFormatException e) {
            // ignore
        }
        return false;
    }

    /**
     * Close a demand
     */
    @Transactional
    public DemandVO closeDemand(Long demandId) {
        Long tenantId = TenantContext.getTenantId();

        RecruitDemand demand = recruitDemandMapper.selectById(demandId);
        if (demand == null) {
            throw new BizException("Demand not found");
        }
        if (!demand.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if ("CLOSED".equals(demand.getStatus())) {
            throw new BizException("Demand is already closed");
        }
        if ("DRAFT".equals(demand.getStatus()) || "PENDING".equals(demand.getStatus())) {
            throw new BizException("Cannot close a demand in DRAFT or PENDING status");
        }

        demand.setStatus("CLOSED");
        recruitDemandMapper.updateById(demand);

        // Cancel any running approval instance
        LambdaUpdateWrapper<ApprovalInstance> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ApprovalInstance::getBizType, "DEMAND")
                .eq(ApprovalInstance::getBizId, demandId)
                .eq(ApprovalInstance::getStatus, "RUNNING")
                .set(ApprovalInstance::getStatus, "CANCELLED");
        approvalInstanceMapper.update(null, updateWrapper);

        return convertToVO(demand);
    }

    /**
     * Get demand detail by ID
     */
    public DemandVO getDemandDetail(Long demandId) {
        Long tenantId = TenantContext.getTenantId();

        RecruitDemand demand = recruitDemandMapper.selectById(demandId);
        if (demand == null) {
            throw new BizException("Demand not found");
        }
        if (!demand.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        return convertToVO(demand);
    }

    /**
     * List demands with pagination and filters
     */
    public PageResult<DemandVO> listDemands(DemandQueryDTO query) {
        Long tenantId = TenantContext.getTenantId();

        Page<RecruitDemand> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<RecruitDemand> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecruitDemand::getTenantId, tenantId);

        if (StringUtils.hasText(query.getTitle())) {
            wrapper.like(RecruitDemand::getTitle, query.getTitle());
        }
        if (query.getOrgId() != null) {
            wrapper.eq(RecruitDemand::getOrgId, query.getOrgId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(RecruitDemand::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getUrgency())) {
            wrapper.eq(RecruitDemand::getUrgency, query.getUrgency());
        }

        wrapper.orderByDesc(RecruitDemand::getCreatedAt);

        Page<RecruitDemand> result = recruitDemandMapper.selectPage(page, wrapper);

        List<DemandVO> voList = new ArrayList<>();
        for (RecruitDemand demand : result.getRecords()) {
            voList.add(convertToVO(demand));
        }

        return new PageResult<>(result.getTotal(), voList, query.getPageNum(), query.getPageSize());
    }

    /**
     * Get my approval list (demands pending my approval)
     */
    public PageResult<DemandVO> getMyApprovals(Long userId, int pageNum, int pageSize) {
        Long tenantId = TenantContext.getTenantId();

        // Find approval instances where current approver is this user
        LambdaQueryWrapper<ApprovalInstance> instanceWrapper = new LambdaQueryWrapper<>();
        instanceWrapper.eq(ApprovalInstance::getTenantId, tenantId)
                .eq(ApprovalInstance::getBizType, "DEMAND")
                .eq(ApprovalInstance::getStatus, "RUNNING")
                .eq(ApprovalInstance::getCurrentApproverId, userId);

        List<ApprovalInstance> instances = approvalInstanceMapper.selectList(instanceWrapper);

        List<DemandVO> voList = new ArrayList<>();
        for (ApprovalInstance instance : instances) {
            RecruitDemand demand = recruitDemandMapper.selectById(instance.getBizId());
            if (demand != null) {
                DemandVO vo = convertToVO(demand);
                vo.setApprovalInstanceId(instance.getId());
                vo.setApprovalStatus(instance.getStatus());
                vo.setCurrentNode(instance.getCurrentNode());
                vo.setCurrentApproverId(instance.getCurrentApproverId());
                voList.add(vo);
            }
        }

        // Simple pagination on the result list
        int total = voList.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<DemandVO> pagedVoList = fromIndex < total ? voList.subList(fromIndex, toIndex) : new ArrayList<DemandVO>();

        return new PageResult<>(total, pagedVoList, pageNum, pageSize);
    }

    /**
     * Get demand board / dashboard statistics
     */
    public DemandBoardVO getDemandBoard() {
        Long tenantId = TenantContext.getTenantId();

        DemandBoardVO board = new DemandBoardVO();

        // Count by status
        board.setTotalDemands(countByStatus(tenantId, null));
        board.setPendingApproval(countByStatus(tenantId, "PENDING"));
        board.setApproved(countByStatus(tenantId, "APPROVED"));
        board.setRecruiting(countByStatus(tenantId, "RECRUITING"));
        board.setCompleted(countByStatus(tenantId, "COMPLETED"));
        board.setClosed(countByStatus(tenantId, "CLOSED"));

        // Aggregate headcount
        LambdaQueryWrapper<RecruitDemand> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecruitDemand::getTenantId, tenantId)
                .in(RecruitDemand::getStatus, "APPROVED", "JOB_CREATED", "RECRUITING", "COMPLETED");
        List<RecruitDemand> activeDemands = recruitDemandMapper.selectList(wrapper);

        int headcountTotal = 0;
        int headcountFilled = 0;
        long overdueCount = 0;
        LocalDate today = LocalDate.now();

        for (RecruitDemand demand : activeDemands) {
            headcountTotal += demand.getApprovedHeadCount() != null ? demand.getApprovedHeadCount() : 0;
            headcountFilled += demand.getFilledCount() != null ? demand.getFilledCount() : 0;
            if (demand.getExpectedOnboardDate() != null && demand.getExpectedOnboardDate().isBefore(today)) {
                overdueCount++;
            }
        }

        board.setHeadcountTotal(headcountTotal);
        board.setHeadcountFilled(headcountFilled);
        board.setOverdueCount(overdueCount);

        return board;
    }

    /**
     * Get approval detail by instance ID
     */
    public ApprovalInstance getApprovalDetail(Long instanceId) {
        Long tenantId = TenantContext.getTenantId();

        ApprovalInstance instance = approvalInstanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new BizException("Approval instance not found");
        }
        if (!instance.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        return instance;
    }

    /**
     * Get approval records for an instance
     */
    public List<ApprovalRecord> getApprovalRecords(Long instanceId) {
        LambdaQueryWrapper<ApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalRecord::getInstanceId, instanceId)
                .orderByAsc(ApprovalRecord::getCreatedAt);
        return approvalRecordMapper.selectList(wrapper);
    }

    /**
     * Count demands by status
     */
    private long countByStatus(Long tenantId, String status) {
        LambdaQueryWrapper<RecruitDemand> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecruitDemand::getTenantId, tenantId);
        if (status != null) {
            wrapper.eq(RecruitDemand::getStatus, status);
        }
        return recruitDemandMapper.selectCount(wrapper);
    }

    /**
     * Generate demand number: DEMAND + yyyyMMdd + 4-digit sequence
     */
    private String generateDemandNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = demandSeqCounter.incrementAndGet();
        // Pad sequence to 4 digits
        String seqStr = String.format("%04d", seq);
        return "DEMAND" + dateStr + seqStr;
    }

    /**
     * Convert entity to VO
     */
    private DemandVO convertToVO(RecruitDemand demand) {
        DemandVO vo = new DemandVO();
        vo.setId(demand.getId());
        vo.setDemandNo(demand.getDemandNo());
        vo.setTitle(demand.getTitle());
        vo.setOrgId(demand.getOrgId());
        vo.setHeadCount(demand.getHeadCount());
        vo.setJobLevel(demand.getJobLevel());
        vo.setSalaryMin(demand.getSalaryMin());
        vo.setSalaryMax(demand.getSalaryMax());
        vo.setUrgency(demand.getUrgency());
        vo.setExpectedOnboardDate(demand.getExpectedOnboardDate());
        vo.setReason(demand.getReason());
        vo.setJobDuty(demand.getJobDuty());
        vo.setJobRequirement(demand.getJobRequirement());
        vo.setWorkLocations(demand.getWorkLocations());
        vo.setReporterId(demand.getReporterId());
        vo.setInitialInterviewerIds(demand.getInitialInterviewerIds());
        vo.setFinalInterviewerIds(demand.getFinalInterviewerIds());
        vo.setStatus(demand.getStatus());
        vo.setApprovedHeadCount(demand.getApprovedHeadCount());
        vo.setFilledCount(demand.getFilledCount());
        vo.setRejectReason(demand.getRejectReason());
        vo.setCreatedBy(demand.getCreatedBy());
        vo.setCreatedAt(demand.getCreatedAt());
        vo.setUpdatedAt(demand.getUpdatedAt());

        // Load approval instance info
        LambdaQueryWrapper<ApprovalInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalInstance::getBizType, "DEMAND")
                .eq(ApprovalInstance::getBizId, demand.getId())
                .orderByDesc(ApprovalInstance::getCreatedAt)
                .last("LIMIT 1");
        ApprovalInstance instance = approvalInstanceMapper.selectOne(wrapper);
        if (instance != null) {
            vo.setApprovalInstanceId(instance.getId());
            vo.setApprovalStatus(instance.getStatus());
            vo.setCurrentApproverId(instance.getCurrentApproverId());
            vo.setCurrentNode(instance.getCurrentNode());
        }

        return vo;
    }
}
