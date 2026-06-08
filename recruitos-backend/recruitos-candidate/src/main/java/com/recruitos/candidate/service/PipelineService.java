package com.recruitos.candidate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recruitos.candidate.dto.*;
import com.recruitos.candidate.entity.*;
import com.recruitos.candidate.mapper.*;
import com.recruitos.common.auth.CurrentUser;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PipelineService {

    private static final List<String> STAGES = Arrays.asList(
            "SOURCED", "SCREENING", "CONTACTED", "INTERVIEWING", "EVALUATED", "OFFER", "HIRED", "ARCHIVED"
    );

    private static final Map<String, String> STAGE_LABELS = new LinkedHashMap<>();

    static {
        STAGE_LABELS.put("SOURCED", "待处理");
        STAGE_LABELS.put("SCREENING", "初筛中");
        STAGE_LABELS.put("CONTACTED", "已联系");
        STAGE_LABELS.put("INTERVIEWING", "面试中");
        STAGE_LABELS.put("EVALUATED", "待录用决策");
        STAGE_LABELS.put("OFFER", "Offer 阶段");
        STAGE_LABELS.put("HIRED", "已入职");
        STAGE_LABELS.put("ARCHIVED", "已结束");
    }

    @Resource
    private CandidateJobMapper candidateJobMapper;
    @Resource
    private CandidateMapper candidateMapper;
    @Resource
    private PipelineStageLogMapper pipelineStageLogMapper;
    @Resource
    private HiringDecisionMapper hiringDecisionMapper;
    @Resource
    private JobPositionReadMapper jobPositionReadMapper;
    @Resource
    private CandidateService candidateService;

    public PipelineBoardVO getBoard(Long jobId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BizException("User not authenticated");
        }

        PipelineBoardVO board = new PipelineBoardVO();
        board.setJobId(jobId);
        if (jobId != null) {
            board.setJobTitle(jobPositionReadMapper.selectTitle(jobId, tenantId));
        }

        LambdaQueryWrapper<CandidateJob> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CandidateJob::getTenantId, tenantId);
        if (jobId != null) {
            wrapper.eq(CandidateJob::getJobId, jobId);
        }
        wrapper.orderByDesc(CandidateJob::getUpdatedAt);
        List<CandidateJob> jobs = candidateJobMapper.selectList(wrapper);

        Map<String, List<CandidateJobVO>> grouped = new LinkedHashMap<>();
        for (String stage : STAGES) {
            grouped.put(stage, new ArrayList<>());
        }
        for (CandidateJob cj : jobs) {
            String stage = StringUtils.hasText(cj.getPipelineStage()) ? cj.getPipelineStage() : "SOURCED";
            if (!grouped.containsKey(stage)) {
                stage = "SOURCED";
            }
            grouped.get(stage).add(candidateService.convertToCandidateJobVO(cj, true));
        }

        List<PipelineBoardVO.StageColumn> columns = new ArrayList<>();
        for (String stage : STAGES) {
            PipelineBoardVO.StageColumn col = new PipelineBoardVO.StageColumn();
            col.setStage(stage);
            col.setLabel(STAGE_LABELS.get(stage));
            col.setItems(grouped.get(stage));
            columns.add(col);
        }
        board.setColumns(columns);
        return board;
    }

    @Transactional
    public CandidateJobVO advanceStage(Long candidateJobId, PipelineAdvanceDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = CurrentUser.getCurrentUserId();
        if (tenantId == null || userId == null) {
            throw new BizException("User not authenticated");
        }

        CandidateJob cj = candidateJobMapper.selectById(candidateJobId);
        if (cj == null || !cj.getTenantId().equals(tenantId)) {
            throw new BizException("Candidate job not found");
        }

        if (!StringUtils.hasText(dto.getToStage())) {
            throw new BizException("Target stage is required");
        }
        if ("ARCHIVED".equals(dto.getToStage()) && !StringUtils.hasText(dto.getReasonCode())) {
            throw new BizException("Rejection reason is required when archiving");
        }

        String fromStage = cj.getPipelineStage();
        String fromSub = cj.getInterviewSubStage();

        cj.setPipelineStage(dto.getToStage());
        if (dto.getToSubStage() != null) {
            cj.setInterviewSubStage(dto.getToSubStage());
        }
        if ("ARCHIVED".equals(dto.getToStage())) {
            cj.setRejectionReasonCode(dto.getReasonCode());
            cj.setRejectionComment(dto.getComment());
            cj.setArchivedToPool(Boolean.TRUE.equals(dto.getArchivedToPool()) ? 1 : 0);
            cj.setScreeningStatus("REJECTED");

            Candidate candidate = candidateMapper.selectById(cj.getCandidateId());
            if (candidate != null) {
                if (Boolean.TRUE.equals(dto.getArchivedToPool())) {
                    candidate.setStatus("POOL");
                } else {
                    candidate.setStatus("BLACKLIST");
                }
                candidateMapper.updateById(candidate);
            }
        } else {
            syncCandidateStatus(cj);
        }
        candidateJobMapper.updateById(cj);

        PipelineStageLog log = new PipelineStageLog();
        log.setTenantId(tenantId);
        log.setCandidateJobId(candidateJobId);
        log.setFromStage(fromStage);
        log.setToStage(dto.getToStage());
        log.setFromSubStage(fromSub);
        log.setToSubStage(dto.getToSubStage());
        log.setOperatorId(userId);
        log.setReasonCode(dto.getReasonCode());
        log.setComment(dto.getComment());
        log.setCreatedAt(LocalDateTime.now());
        pipelineStageLogMapper.insert(log);

        return candidateService.convertToCandidateJobVO(cj);
    }

    private void syncCandidateStatus(CandidateJob cj) {
        Candidate candidate = candidateMapper.selectById(cj.getCandidateId());
        if (candidate == null) {
            return;
        }
        String stage = cj.getPipelineStage();
        String status;
        switch (stage) {
            case "SCREENING": status = "SCREENING"; break;
            case "CONTACTED": status = "SCREENING"; break;
            case "INTERVIEWING": status = "INTERVIEWING"; break;
            case "EVALUATED": status = "INTERVIEWING"; break;
            case "OFFER": status = "OFFER"; break;
            case "HIRED": status = "ONBOARD"; break;
            default: status = "NEW"; break;
        }
        candidate.setStatus(status);
        candidateMapper.updateById(candidate);
    }

    @Transactional
    public HiringDecision saveHiringDecision(HiringDecisionDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = CurrentUser.getCurrentUserId();
        if (tenantId == null || userId == null) {
            throw new BizException("User not authenticated");
        }

        LambdaQueryWrapper<HiringDecision> exist = new LambdaQueryWrapper<>();
        exist.eq(HiringDecision::getCandidateJobId, dto.getCandidateJobId());
        HiringDecision existing = hiringDecisionMapper.selectOne(exist);

        HiringDecision decision = existing != null ? existing : new HiringDecision();
        decision.setTenantId(tenantId);
        decision.setCandidateJobId(dto.getCandidateJobId());
        decision.setCandidateId(dto.getCandidateId());
        decision.setJobId(dto.getJobId());
        decision.setDecision(dto.getDecision());
        decision.setSummary(dto.getSummary());
        decision.setDecidedBy(userId);
        decision.setDecidedAt(LocalDateTime.now());

        if (existing != null) {
            hiringDecisionMapper.updateById(decision);
        } else {
            hiringDecisionMapper.insert(decision);
        }

        if ("HIRE".equals(dto.getDecision())) {
            PipelineAdvanceDTO advance = new PipelineAdvanceDTO();
            advance.setToStage("EVALUATED");
            advance.setComment("Hiring decision: HIRE");
            CandidateJob cj = candidateJobMapper.selectById(dto.getCandidateJobId());
            if (cj != null && !"OFFER".equals(cj.getPipelineStage()) && !"HIRED".equals(cj.getPipelineStage())) {
                cj.setPipelineStage("EVALUATED");
                candidateJobMapper.updateById(cj);
            }
        }

        return decision;
    }

    public boolean hasHireDecision(Long candidateJobId) {
        LambdaQueryWrapper<HiringDecision> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HiringDecision::getCandidateJobId, candidateJobId)
                .eq(HiringDecision::getDecision, "HIRE");
        return hiringDecisionMapper.selectCount(wrapper) > 0;
    }

    public Map<String, Object> getCandidate360(Long candidateId) {
        Long tenantId = TenantContext.getTenantId();
        CandidateVO candidate = candidateService.getCandidateDetail(candidateId);

        LambdaQueryWrapper<CandidateJob> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CandidateJob::getTenantId, tenantId)
                .eq(CandidateJob::getCandidateId, candidateId)
                .orderByDesc(CandidateJob::getUpdatedAt);
        List<CandidateJob> jobs = candidateJobMapper.selectList(wrapper);

        List<CandidateJobVO> jobVOs = new ArrayList<>();
        for (CandidateJob cj : jobs) {
            jobVOs.add(candidateService.convertToCandidateJobVO(cj, true));
        }

        List<PipelineStageLog> logs = Collections.emptyList();
        if (!jobs.isEmpty()) {
            List<Long> cjIds = new ArrayList<>();
            for (CandidateJob cj : jobs) {
                cjIds.add(cj.getId());
            }
            LambdaQueryWrapper<PipelineStageLog> logWrapper = new LambdaQueryWrapper<>();
            logWrapper.eq(PipelineStageLog::getTenantId, tenantId)
                    .in(PipelineStageLog::getCandidateJobId, cjIds)
                    .orderByDesc(PipelineStageLog::getCreatedAt);
            logs = pipelineStageLogMapper.selectList(logWrapper);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("candidate", candidate);
        result.put("jobs", jobVOs);
        result.put("timeline", logs);
        return result;
    }
}
