package com.recruitos.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.interview.dto.*;
import com.recruitos.interview.entity.Interview;
import com.recruitos.interview.entity.InterviewEvaluation;
import com.recruitos.interview.entity.InterviewSlot;
import com.recruitos.interview.mapper.CandidateMapper;
import com.recruitos.interview.mapper.InterviewEvaluationMapper;
import com.recruitos.interview.mapper.InterviewMapper;
import com.recruitos.interview.mapper.InterviewSlotMapper;
import com.recruitos.interview.mapper.JobPositionMapper;
import com.recruitos.interview.mapper.RecruitDemandMapper;
import com.recruitos.interview.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Interview service - core business logic for interview management
 *
 * Status flow:
 * PENDING_ARRANGE -> ARRANGED -> IN_PROGRESS -> COMPLETED
 *                                   |
 *                             CANCELLED / NO_SHOW
 */
@Service
public class InterviewService {

    @Resource
    private InterviewMapper interviewMapper;

    @Resource
    private InterviewEvaluationMapper evaluationMapper;

    @Resource
    private InterviewSlotMapper slotMapper;

    @Resource
    private CandidateMapper candidateMapper;

    @Resource
    private JobPositionMapper jobPositionMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private RecruitDemandMapper recruitDemandMapper;

    /**
     * Arrange an interview - create interview record and candidate time slots
     */
    @Transactional
    public InterviewVO arrangeInterview(InterviewArrangeDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BizException("User not authenticated");
        }

        // Create interview record
        Interview interview = new Interview();
        interview.setTenantId(tenantId);
        interview.setCandidateId(dto.getCandidateId());
        interview.setJobId(dto.getJobId());
        interview.setDemandId(dto.getDemandId());
        interview.setRound(dto.getRound());
        interview.setRoundSeq(calculateRoundSeq(tenantId, dto.getCandidateId(), dto.getJobId(), dto.getRound()));
        interview.setInterviewerId(dto.getInterviewerId());
        interview.setFormat(dto.getFormat());
        interview.setMeetingPlatform(dto.getMeetingPlatform());
        interview.setLocation(dto.getLocation());
        interview.setDurationMinutes(dto.getDurationMinutes());
        interview.setStatus("PENDING_ARRANGE");

        interviewMapper.insert(interview);

        // Create candidate time slots
        if (dto.getSlots() != null && !dto.getSlots().isEmpty()) {
            for (InterviewSlotDTO slotDTO : dto.getSlots()) {
                InterviewSlot slot = new InterviewSlot();
                slot.setTenantId(tenantId);
                slot.setInterviewId(interview.getId());
                slot.setSlotStart(slotDTO.getSlotStart());
                slot.setSlotEnd(slotDTO.getSlotEnd());
                slot.setIsSelected(false);
                slotMapper.insert(slot);
            }
        }

        return convertToVO(interview);
    }

    /**
     * Confirm interview time - select a slot and update interview status to ARRANGED
     */
    @Transactional
    public InterviewVO confirmInterview(Long interviewId, Long slotId) {
        Long tenantId = TenantContext.getTenantId();

        Interview interview = interviewMapper.selectById(interviewId);
        if (interview == null) {
            throw new BizException("Interview not found");
        }
        if (!interview.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if (!"PENDING_ARRANGE".equals(interview.getStatus())) {
            throw new BizException("Interview is not in PENDING_ARRANGE status");
        }

        InterviewSlot selectedSlot = slotMapper.selectById(slotId);
        if (selectedSlot == null) {
            throw new BizException("Slot not found");
        }
        if (!selectedSlot.getInterviewId().equals(interviewId)) {
            throw new BizException("Slot does not belong to this interview");
        }

        // Deselect all other slots for this interview
        LambdaQueryWrapper<InterviewSlot> slotWrapper = new LambdaQueryWrapper<>();
        slotWrapper.eq(InterviewSlot::getInterviewId, interviewId);
        List<InterviewSlot> allSlots = slotMapper.selectList(slotWrapper);
        for (InterviewSlot slot : allSlots) {
            slot.setIsSelected(slot.getId().equals(slotId));
            slotMapper.updateById(slot);
        }

        // Update interview with confirmed time
        interview.setScheduledStartTime(selectedSlot.getSlotStart());
        interview.setScheduledEndTime(selectedSlot.getSlotEnd());
        interview.setStatus("ARRANGED");
        interviewMapper.updateById(interview);

        return convertToVO(interview);
    }

    /**
     * Start interview - change status to IN_PROGRESS and record actual start time
     */
    @Transactional
    public InterviewVO startInterview(Long interviewId) {
        Long tenantId = TenantContext.getTenantId();

        Interview interview = interviewMapper.selectById(interviewId);
        if (interview == null) {
            throw new BizException("Interview not found");
        }
        if (!interview.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if (!"ARRANGED".equals(interview.getStatus())) {
            throw new BizException("Interview is not in ARRANGED status");
        }

        interview.setStatus("IN_PROGRESS");
        interview.setActualStartTime(LocalDateTime.now());
        interviewMapper.updateById(interview);

        return convertToVO(interview);
    }

    /**
     * Complete interview - change status to COMPLETED and record actual end time
     */
    @Transactional
    public InterviewVO completeInterview(Long interviewId) {
        Long tenantId = TenantContext.getTenantId();

        Interview interview = interviewMapper.selectById(interviewId);
        if (interview == null) {
            throw new BizException("Interview not found");
        }
        if (!interview.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if (!"IN_PROGRESS".equals(interview.getStatus())) {
            throw new BizException("Interview is not in IN_PROGRESS status");
        }

        interview.setStatus("COMPLETED");
        interview.setActualEndTime(LocalDateTime.now());
        interviewMapper.updateById(interview);

        return convertToVO(interview);
    }

    /**
     * Cancel interview
     */
    @Transactional
    public InterviewVO cancelInterview(Long interviewId, String reason) {
        Long tenantId = TenantContext.getTenantId();

        Interview interview = interviewMapper.selectById(interviewId);
        if (interview == null) {
            throw new BizException("Interview not found");
        }
        if (!interview.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if ("COMPLETED".equals(interview.getStatus()) || "CANCELLED".equals(interview.getStatus())) {
            throw new BizException("Cannot cancel a completed or already cancelled interview");
        }

        interview.setStatus("CANCELLED");
        interview.setCancelReason(reason);
        interviewMapper.updateById(interview);

        return convertToVO(interview);
    }

    /**
     * Submit evaluation for an interview
     */
    @Transactional
    public InterviewVO submitEvaluation(EvaluationSubmitDTO dto) {
        Long tenantId = TenantContext.getTenantId();

        Interview interview = interviewMapper.selectById(dto.getInterviewId());
        if (interview == null) {
            throw new BizException("Interview not found");
        }
        if (!interview.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if (!"COMPLETED".equals(interview.getStatus())) {
            throw new BizException("Can only submit evaluation for completed interviews");
        }

        // Check if evaluation already exists
        LambdaQueryWrapper<InterviewEvaluation> evalWrapper = new LambdaQueryWrapper<>();
        evalWrapper.eq(InterviewEvaluation::getInterviewId, dto.getInterviewId());
        Long existingCount = evaluationMapper.selectCount(evalWrapper);
        if (existingCount > 0) {
            throw new BizException("Evaluation already submitted for this interview");
        }

        InterviewEvaluation evaluation = new InterviewEvaluation();
        evaluation.setTenantId(tenantId);
        evaluation.setInterviewId(dto.getInterviewId());
        evaluation.setRound(interview.getRound());
        evaluation.setDecision(dto.getDecision());
        evaluation.setOverallScore(dto.getOverallScore());
        evaluation.setDimensions(dto.getDimensions());
        evaluation.setEvolutionFeedback(dto.getEvolutionFeedback());
        evaluation.setComment(dto.getComment());
        evaluation.setSubmittedAt(LocalDateTime.now());

        evaluationMapper.insert(evaluation);

        // Trigger next round if initial interview passed
        if ("INITIAL".equals(interview.getRound()) && "PASS".equals(dto.getDecision())) {
            triggerNextRound(interview);
        }

        return convertToVO(interview);
    }

    /**
     * Get interview detail by ID
     */
    public InterviewVO getInterviewDetail(Long interviewId) {
        Long tenantId = TenantContext.getTenantId();

        Interview interview = interviewMapper.selectById(interviewId);
        if (interview == null) {
            throw new BizException("Interview not found");
        }
        if (!interview.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        return convertToVO(interview);
    }

    /**
     * List interviews with pagination and filters
     */
    public PageResult<InterviewVO> listInterviews(InterviewQueryDTO query) {
        Long tenantId = TenantContext.getTenantId();

        Page<Interview> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<Interview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Interview::getTenantId, tenantId);

        if (query.getCandidateId() != null) {
            wrapper.eq(Interview::getCandidateId, query.getCandidateId());
        }
        if (query.getJobId() != null) {
            wrapper.eq(Interview::getJobId, query.getJobId());
        }
        if (StringUtils.hasText(query.getRound())) {
            wrapper.eq(Interview::getRound, query.getRound());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Interview::getStatus, query.getStatus());
        }
        if (query.getInterviewerId() != null) {
            wrapper.eq(Interview::getInterviewerId, query.getInterviewerId());
        }

        wrapper.orderByDesc(Interview::getCreatedAt);

        Page<Interview> result = interviewMapper.selectPage(page, wrapper);

        List<InterviewVO> voList = new ArrayList<>();
        for (Interview interview : result.getRecords()) {
            voList.add(convertToVO(interview));
        }

        return new PageResult<>(result.getTotal(), voList, query.getPageNum(), query.getPageSize());
    }

    /**
     * Get interview calendar for an interviewer within a date range
     */
    public List<InterviewCalendarVO> getInterviewCalendar(Long interviewerId, LocalDateTime startDate, LocalDateTime endDate) {
        Long tenantId = TenantContext.getTenantId();

        LambdaQueryWrapper<Interview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Interview::getTenantId, tenantId)
                .eq(Interview::getInterviewerId, interviewerId)
                .in(Interview::getStatus, "ARRANGED", "IN_PROGRESS", "COMPLETED")
                .ge(Interview::getScheduledStartTime, startDate)
                .le(Interview::getScheduledStartTime, endDate)
                .orderByAsc(Interview::getScheduledStartTime);

        List<Interview> interviews = interviewMapper.selectList(wrapper);

        List<InterviewCalendarVO> calendarList = new ArrayList<>();
        for (Interview interview : interviews) {
            InterviewCalendarVO vo = new InterviewCalendarVO();
            vo.setInterviewId(interview.getId());
            vo.setCandidateName(resolveCandidateName(interview.getCandidateId()));
            vo.setJobTitle(resolveJobTitle(interview.getJobId()));
            vo.setInterviewerName(resolveInterviewerName(interview.getInterviewerId()));
            vo.setStartTime(interview.getScheduledStartTime());
            vo.setEndTime(interview.getScheduledEndTime());
            vo.setRound(interview.getRound());
            vo.setStatus(interview.getStatus());
            vo.setFormat(interview.getFormat());
            calendarList.add(vo);
        }

        return calendarList;
    }

    /**
     * Get interviews assigned to a specific interviewer
     */
    public PageResult<InterviewVO> getMyInterviews(Long interviewerId, int pageNum, int pageSize) {
        Long tenantId = TenantContext.getTenantId();

        InterviewQueryDTO query = new InterviewQueryDTO();
        query.setInterviewerId(interviewerId);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);

        return listInterviews(query);
    }

    /**
     * Trigger next round (FINAL) after initial interview passes
     * Called automatically when evaluation decision is PASS for INITIAL round
     */
    public InterviewVO triggerNextRound(Long interviewId) {
        Long tenantId = TenantContext.getTenantId();

        Interview interview = interviewMapper.selectById(interviewId);
        if (interview == null) {
            throw new BizException("Interview not found");
        }
        if (!interview.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        // Verify the interview is INITIAL round and has a PASS evaluation
        if (!"INITIAL".equals(interview.getRound())) {
            throw new BizException("Can only trigger next round for INITIAL interviews");
        }

        LambdaQueryWrapper<InterviewEvaluation> evalWrapper = new LambdaQueryWrapper<>();
        evalWrapper.eq(InterviewEvaluation::getInterviewId, interviewId)
                .eq(InterviewEvaluation::getDecision, "PASS");
        Long passCount = evaluationMapper.selectCount(evalWrapper);
        if (passCount == 0) {
            throw new BizException("Initial interview has not passed yet");
        }

        triggerNextRound(interview);
        return convertToVO(interview);
    }

    /**
     * Internal method to trigger next round interview
     */
    private void triggerNextRound(Interview interview) {
        Long tenantId = interview.getTenantId();

        // Check if FINAL round already exists for this candidate+job
        LambdaQueryWrapper<Interview> existingWrapper = new LambdaQueryWrapper<>();
        existingWrapper.eq(Interview::getTenantId, tenantId)
                .eq(Interview::getCandidateId, interview.getCandidateId())
                .eq(Interview::getJobId, interview.getJobId())
                .eq(Interview::getRound, "FINAL");
        Long existingCount = interviewMapper.selectCount(existingWrapper);
        if (existingCount > 0) {
            // Final round already exists, skip
            return;
        }

        // Create FINAL round interview record
        Interview finalInterview = new Interview();
        finalInterview.setTenantId(tenantId);
        finalInterview.setCandidateId(interview.getCandidateId());
        finalInterview.setJobId(interview.getJobId());
        finalInterview.setDemandId(interview.getDemandId());
        finalInterview.setRound("FINAL");
        finalInterview.setRoundSeq(1);
        finalInterview.setInterviewerId(resolveFinalInterviewer(interview));
        finalInterview.setFormat(interview.getFormat());
        finalInterview.setMeetingPlatform(interview.getMeetingPlatform());
        finalInterview.setLocation(interview.getLocation());
        finalInterview.setDurationMinutes(interview.getDurationMinutes());
        finalInterview.setStatus("PENDING_ARRANGE");

        interviewMapper.insert(finalInterview);

        // TODO: Notify candidate and interviewer about the final round
        // notificationService.notifyCandidate(finalInterview.getCandidateId(), "Please select time for final interview");
        // notificationService.notifyInterviewer(finalInterview.getInterviewerId(), "New final round interview assigned");
    }

    /**
     * Calculate round sequence number for the same candidate+job+round
     */
    private Integer calculateRoundSeq(Long tenantId, Long candidateId, Long jobId, String round) {
        LambdaQueryWrapper<Interview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Interview::getTenantId, tenantId)
                .eq(Interview::getCandidateId, candidateId)
                .eq(Interview::getJobId, jobId)
                .eq(Interview::getRound, round);
        Long count = interviewMapper.selectCount(wrapper);
        return count.intValue() + 1;
    }

    /**
     * Resolve final interviewer ID
     * In a real system this would look up from the demand's finalInterviewerIds
     */
    private Long resolveFinalInterviewer(Interview interview) {
        if (interview.getDemandId() == null) return null;
        String ids = recruitDemandMapper.selectFinalInterviewerIdsById(interview.getDemandId());
        if (ids == null || ids.isEmpty()) return null;
        // Return the first final interviewer ID
        try {
            String[] parts = ids.split(",");
            return Long.parseLong(parts[0].trim());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Resolve candidate name (placeholder - in production would call candidate service)
     */
    private String resolveCandidateName(Long candidateId) {
        if (candidateId == null) return null;
        String name = candidateMapper.selectNameById(candidateId);
        return name != null ? name : "Candidate#" + candidateId;
    }

    /**
     * Resolve job title (placeholder - in production would call job service)
     */
    private String resolveJobTitle(Long jobId) {
        if (jobId == null) return null;
        String title = jobPositionMapper.selectTitleById(jobId);
        return title != null ? title : "Job#" + jobId;
    }

    /**
     * Resolve interviewer name (placeholder - in production would call user service)
     */
    private String resolveInterviewerName(Long interviewerId) {
        if (interviewerId == null) return null;
        String name = sysUserMapper.selectRealNameById(interviewerId);
        return name != null ? name : "Interviewer#" + interviewerId;
    }

    /**
     * Convert Interview entity to InterviewVO with slots and evaluation
     */
    private InterviewVO convertToVO(Interview interview) {
        InterviewVO vo = new InterviewVO();
        vo.setId(interview.getId());
        vo.setCandidateId(interview.getCandidateId());
        vo.setCandidateName(resolveCandidateName(interview.getCandidateId()));
        vo.setJobId(interview.getJobId());
        vo.setJobTitle(resolveJobTitle(interview.getJobId()));
        vo.setDemandId(interview.getDemandId());
        vo.setRound(interview.getRound());
        vo.setRoundSeq(interview.getRoundSeq());
        vo.setInterviewerId(interview.getInterviewerId());
        vo.setInterviewerName(resolveInterviewerName(interview.getInterviewerId()));
        vo.setScheduledStartTime(interview.getScheduledStartTime());
        vo.setScheduledEndTime(interview.getScheduledEndTime());
        vo.setActualStartTime(interview.getActualStartTime());
        vo.setActualEndTime(interview.getActualEndTime());
        vo.setFormat(interview.getFormat());
        vo.setMeetingLink(interview.getMeetingLink());
        vo.setMeetingPlatform(interview.getMeetingPlatform());
        vo.setLocation(interview.getLocation());
        vo.setDurationMinutes(interview.getDurationMinutes());
        vo.setStatus(interview.getStatus());
        vo.setRecordingUrl(interview.getRecordingUrl());
        vo.setAiSummary(interview.getAiSummary());
        vo.setCandidateAgreedRecord(interview.getCandidateAgreedRecord());
        vo.setCancelReason(interview.getCancelReason());
        vo.setCreatedAt(interview.getCreatedAt());
        vo.setUpdatedAt(interview.getUpdatedAt());

        // Load slots
        LambdaQueryWrapper<InterviewSlot> slotWrapper = new LambdaQueryWrapper<>();
        slotWrapper.eq(InterviewSlot::getInterviewId, interview.getId());
        List<InterviewSlot> slots = slotMapper.selectList(slotWrapper);
        List<InterviewSlotDTO> slotDTOs = new ArrayList<>();
        for (InterviewSlot slot : slots) {
            InterviewSlotDTO slotDTO = new InterviewSlotDTO();
            slotDTO.setSlotStart(slot.getSlotStart());
            slotDTO.setSlotEnd(slot.getSlotEnd());
            slotDTOs.add(slotDTO);
        }
        vo.setSlots(slotDTOs);

        // Load evaluation
        LambdaQueryWrapper<InterviewEvaluation> evalWrapper = new LambdaQueryWrapper<>();
        evalWrapper.eq(InterviewEvaluation::getInterviewId, interview.getId());
        InterviewEvaluation evaluation = evaluationMapper.selectOne(evalWrapper);
        if (evaluation != null) {
            EvaluationSubmitDTO evalDTO = new EvaluationSubmitDTO();
            evalDTO.setInterviewId(evaluation.getInterviewId());
            evalDTO.setDecision(evaluation.getDecision());
            evalDTO.setOverallScore(evaluation.getOverallScore());
            evalDTO.setDimensions(evaluation.getDimensions());
            evalDTO.setEvolutionFeedback(evaluation.getEvolutionFeedback());
            evalDTO.setComment(evaluation.getComment());
            vo.setEvaluation(evalDTO);
        }

        return vo;
    }
}
