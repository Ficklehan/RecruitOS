package com.recruitos.candidate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruitos.candidate.dto.*;
import com.recruitos.candidate.entity.Candidate;
import com.recruitos.candidate.entity.CandidateJob;
import com.recruitos.candidate.entity.Resume;
import com.recruitos.candidate.mapper.CandidateJobMapper;
import com.recruitos.candidate.mapper.CandidateMapper;
import com.recruitos.candidate.mapper.JobPositionReadMapper;
import com.recruitos.candidate.mapper.ResumeMapper;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.match.TagMatchScoreCalculator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Candidate service - core business logic for candidate management
 */
@Service
public class CandidateService {

    private static final Logger log = LoggerFactory.getLogger(CandidateService.class);

    @Resource
    private CandidateMapper candidateMapper;

    @Resource
    private ResumeMapper resumeMapper;

    @Resource
    private CandidateJobMapper candidateJobMapper;

    @Resource
    private MatchVerdictService matchVerdictService;

    @Resource
    private JobPositionReadMapper jobPositionReadMapper;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * Create a new candidate
     */
    @Transactional
    public CandidateVO createCandidate(CandidateCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BizException("User not authenticated");
        }

        Candidate candidate = new Candidate();
        candidate.setName(dto.getName());
        candidate.setPhone(dto.getPhone());
        candidate.setEmail(dto.getEmail());
        candidate.setGender(dto.getGender());
        candidate.setCurrentCompany(dto.getCurrentCompany());
        candidate.setCurrentTitle(dto.getCurrentTitle());
        candidate.setWorkYears(dto.getWorkYears());
        candidate.setEducation(dto.getEducation());
        candidate.setSchool(dto.getSchool());
        candidate.setMajor(dto.getMajor());
        candidate.setExpectedSalary(dto.getExpectedSalary());
        candidate.setWorkLocation(dto.getWorkLocation());
        candidate.setSource(dto.getSource());
        candidate.setSourceDetail(dto.getSourceDetail());
        candidate.setStatus("NEW");

        candidateMapper.insert(candidate);

        return convertToVO(candidate);
    }

    /**
     * Update an existing candidate
     */
    @Transactional
    public CandidateVO updateCandidate(Long id, CandidateCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();

        Candidate candidate = candidateMapper.selectById(id);
        if (candidate == null) {
            throw new BizException("Candidate not found");
        }
        if (!candidate.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        candidate.setName(dto.getName());
        candidate.setPhone(dto.getPhone());
        candidate.setEmail(dto.getEmail());
        candidate.setGender(dto.getGender());
        candidate.setCurrentCompany(dto.getCurrentCompany());
        candidate.setCurrentTitle(dto.getCurrentTitle());
        candidate.setWorkYears(dto.getWorkYears());
        candidate.setEducation(dto.getEducation());
        candidate.setSchool(dto.getSchool());
        candidate.setMajor(dto.getMajor());
        candidate.setExpectedSalary(dto.getExpectedSalary());
        candidate.setWorkLocation(dto.getWorkLocation());
        candidate.setSource(dto.getSource());
        candidate.setSourceDetail(dto.getSourceDetail());

        candidateMapper.updateById(candidate);

        return convertToVO(candidate);
    }

    /**
     * Get candidate detail with associated jobs
     */
    public CandidateVO getCandidateDetail(Long id) {
        Long tenantId = TenantContext.getTenantId();

        Candidate candidate = candidateMapper.selectById(id);
        if (candidate == null) {
            throw new BizException("Candidate not found");
        }
        if (!candidate.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        CandidateVO vo = convertToVO(candidate);

        // Load associated jobs
        LambdaQueryWrapper<CandidateJob> jobWrapper = new LambdaQueryWrapper<>();
        jobWrapper.eq(CandidateJob::getCandidateId, id)
                .eq(CandidateJob::getTenantId, tenantId)
                .orderByDesc(CandidateJob::getMatchScore);
        List<CandidateJob> jobs = candidateJobMapper.selectList(jobWrapper);

        List<CandidateJobVO> jobVOs = new ArrayList<>();
        for (CandidateJob job : jobs) {
            jobVOs.add(convertToCandidateJobVO(job, true));
        }
        vo.setJobs(jobVOs);

        return vo;
    }

    /**
     * List candidates with pagination and filters
     */
    public PageResult<CandidateVO> listCandidates(CandidateQueryDTO query) {
        Long tenantId = TenantContext.getTenantId();

        Page<Candidate> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<Candidate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Candidate::getTenantId, tenantId);

        if (StringUtils.hasText(query.getName())) {
            wrapper.like(Candidate::getName, query.getName());
        }
        if (StringUtils.hasText(query.getPhone())) {
            wrapper.like(Candidate::getPhone, query.getPhone());
        }
        if (StringUtils.hasText(query.getCompany())) {
            wrapper.like(Candidate::getCurrentCompany, query.getCompany());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Candidate::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getSource())) {
            wrapper.eq(Candidate::getSource, query.getSource());
        }

        // If jobId filter is specified, find candidate IDs from candidate_job table first
        if (query.getJobId() != null) {
            LambdaQueryWrapper<CandidateJob> cjWrapper = new LambdaQueryWrapper<>();
            cjWrapper.eq(CandidateJob::getTenantId, tenantId)
                    .eq(CandidateJob::getJobId, query.getJobId())
                    .select(CandidateJob::getCandidateId);
            List<CandidateJob> cjList = candidateJobMapper.selectList(cjWrapper);
            List<Long> candidateIds = new ArrayList<>();
            for (CandidateJob cj : cjList) {
                candidateIds.add(cj.getCandidateId());
            }
            if (candidateIds.isEmpty()) {
                return new PageResult<>(0, new ArrayList<CandidateVO>(), query.getPageNum(), query.getPageSize());
            }
            wrapper.in(Candidate::getId, candidateIds);
        }

        wrapper.orderByDesc(Candidate::getCreatedAt);

        Page<Candidate> result = candidateMapper.selectPage(page, wrapper);

        Long filterJobId = query.getJobId();
        List<CandidateVO> voList = new ArrayList<>();
        for (Candidate candidate : result.getRecords()) {
            CandidateVO vo = convertToVO(candidate);
            if (filterJobId != null) {
                enrichMatchForJob(vo, candidate, filterJobId, tenantId);
            }
            voList.add(vo);
        }

        return new PageResult<>(result.getTotal(), voList, query.getPageNum(), query.getPageSize());
    }

    /**
     * Add candidate to a job with auto match score calculation
     */
    @Transactional
    public CandidateJobVO addCandidateToJob(Long candidateId, Long jobId) {
        Long tenantId = TenantContext.getTenantId();

        Candidate candidate = candidateMapper.selectById(candidateId);
        if (candidate == null) {
            throw new BizException("Candidate not found");
        }
        if (!candidate.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        // Idempotent: return existing association instead of failing
        LambdaQueryWrapper<CandidateJob> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(CandidateJob::getTenantId, tenantId)
                .eq(CandidateJob::getCandidateId, candidateId)
                .eq(CandidateJob::getJobId, jobId);
        CandidateJob existing = candidateJobMapper.selectOne(existWrapper);
        if (existing != null) {
            log.info("Candidate {} already linked to job {}, returning existing association", candidateId, jobId);
            return convertToCandidateJobVO(existing);
        }

        BigDecimal matchScore = calculateMatchScore(candidateId, jobId);
        String matchDetail = matchVerdictService.buildMatchDetailJson(
                tenantId, candidateId, jobId, candidate, matchScore);

        CandidateJob candidateJob = new CandidateJob();
        candidateJob.setCandidateId(candidateId);
        candidateJob.setJobId(jobId);
        candidateJob.setMatchScore(matchScore);
        candidateJob.setMatchDetail(matchDetail);
        candidateJob.setScreeningStatus("PENDING");
        candidateJob.setPipelineStage("SOURCED");

        candidateJobMapper.insert(candidateJob);

        return convertToCandidateJobVO(candidateJob);
    }

    /**
     * Screening operation: pass / reject / reserve
     */
    @Transactional
    public CandidateJobVO screening(Long candidateId, Long jobId, ScreeningDTO dto) {
        Long tenantId = TenantContext.getTenantId();

        LambdaQueryWrapper<CandidateJob> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CandidateJob::getTenantId, tenantId)
                .eq(CandidateJob::getCandidateId, candidateId)
                .eq(CandidateJob::getJobId, jobId);
        CandidateJob candidateJob = candidateJobMapper.selectOne(wrapper);
        if (candidateJob == null) {
            throw new BizException("Candidate is not associated with this job");
        }

        candidateJob.setScreeningStatus(dto.getScreeningStatus());
        candidateJob.setScreenerComment(dto.getScreenerComment());

        candidateJobMapper.updateById(candidateJob);

        return convertToCandidateJobVO(candidateJob);
    }

    /**
     * Get decision panel data: match score breakdown for a candidate-job pair
     */
    public CandidateJobVO getDecisionPanel(Long candidateId, Long jobId) {
        Long tenantId = TenantContext.getTenantId();

        LambdaQueryWrapper<CandidateJob> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CandidateJob::getTenantId, tenantId)
                .eq(CandidateJob::getCandidateId, candidateId)
                .eq(CandidateJob::getJobId, jobId);
        CandidateJob candidateJob = candidateJobMapper.selectOne(wrapper);
        if (candidateJob == null) {
            throw new BizException("Candidate is not associated with this job");
        }

        Candidate candidate = candidateMapper.selectById(candidateId);
        BigDecimal score = candidateJob.getMatchScore() != null
                ? candidateJob.getMatchScore()
                : calculateMatchScore(candidateId, jobId);
        String detail = matchVerdictService.buildMatchDetailJson(
                tenantId, candidateId, jobId, candidate, score);
        candidateJob.setMatchScore(score);
        candidateJob.setMatchDetail(detail);
        candidateJobMapper.updateById(candidateJob);

        return convertToCandidateJobVO(candidateJob);
    }

    /**
     * Talent pool: default browse shows candidates not in an active pipeline;
     * keyword/tags search scans the broader pool.
     */
    public PageResult<CandidateVO> getTalentPool(String keyword, String tags, Long jobId,
                                                  Integer pageNum, Integer pageSize) {
        Long tenantId = TenantContext.getTenantId();

        int pn = pageNum != null && pageNum > 0 ? pageNum : 1;
        int ps = pageSize != null && pageSize > 0 ? pageSize : 12;
        Page<Candidate> page = new Page<>(pn, ps);

        LambdaQueryWrapper<Candidate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Candidate::getTenantId, tenantId);
        wrapper.ne(Candidate::getStatus, "BLACKLIST");

        boolean hasSearch = StringUtils.hasText(keyword) || StringUtils.hasText(tags);
        if (!hasSearch) {
            applyNotInActivePipelineFilter(wrapper, tenantId);
        }

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(Candidate::getName, keyword)
                    .or().like(Candidate::getPhone, keyword)
                    .or().like(Candidate::getEmail, keyword)
                    .or().like(Candidate::getCurrentCompany, keyword)
                    .or().like(Candidate::getCurrentTitle, keyword)
                    .or().like(Candidate::getSchool, keyword)
            );
        }
        if (StringUtils.hasText(tags)) {
            wrapper.like(Candidate::getTags, tags);
        }

        wrapper.orderByDesc(Candidate::getUpdatedAt);

        Page<Candidate> result = candidateMapper.selectPage(page, wrapper);

        List<CandidateVO> voList = new ArrayList<>();
        for (Candidate candidate : result.getRecords()) {
            CandidateVO vo = convertToVO(candidate);
            if (jobId != null) {
                enrichMatchForJob(vo, candidate, jobId, tenantId);
            }
            voList.add(vo);
        }

        return new PageResult<>(result.getTotal(), voList, pn, ps);
    }

    /** Reserved for pool or not linked to any non-archived job pipeline. */
    private void applyNotInActivePipelineFilter(LambdaQueryWrapper<Candidate> wrapper, Long tenantId) {
        wrapper.and(w -> w.eq(Candidate::getStatus, "POOL")
                .or()
                .apply("NOT EXISTS (SELECT 1 FROM candidate_job cj WHERE cj.candidate_id = candidate.id"
                        + " AND cj.tenant_id = {0}"
                        + " AND IFNULL(cj.pipeline_stage, 'SOURCED') <> 'ARCHIVED')", tenantId));
    }

    private void enrichMatchForJob(CandidateVO vo, Candidate candidate, Long jobId, Long tenantId) {
        vo.setContextJobId(jobId);
        LambdaQueryWrapper<CandidateJob> w = new LambdaQueryWrapper<>();
        w.eq(CandidateJob::getTenantId, tenantId)
                .eq(CandidateJob::getCandidateId, candidate.getId())
                .eq(CandidateJob::getJobId, jobId);
        CandidateJob cj = candidateJobMapper.selectOne(w);
        BigDecimal score;
        if (cj != null && cj.getMatchScore() != null) {
            score = cj.getMatchScore();
        } else {
            score = calculateMatchScore(candidate.getId(), jobId);
        }
        vo.setMatchScore(score);
        vo.setMatchDetail(matchVerdictService.buildMatchDetailJson(
                tenantId, candidate.getId(), jobId, candidate, score));
        if (cj != null && StringUtils.hasText(cj.getPipelineStage())) {
            vo.setPipelineStage(cj.getPipelineStage());
        }
    }

    /**
     * 岗位 JD 标签与候选人技能画像的统一匹配分（与 agent 筛选、决策面板一致）。
     */
    private BigDecimal calculateMatchScore(Long candidateId, Long jobId) {
        Candidate candidate = candidateMapper.selectById(candidateId);
        if (candidate == null || jobId == null) {
            return BigDecimal.ZERO;
        }
        Long tenantId = TenantContext.getTenantId();
        String jobTags = jobPositionReadMapper.selectTags(jobId, tenantId);

        TagMatchScoreCalculator.CandidateSkillProfile profile = new TagMatchScoreCalculator.CandidateSkillProfile();
        profile.setTags(combineCandidateTags(candidate.getTags(), extractResumeSkills(candidateId)));
        profile.setCurrentTitle(candidate.getCurrentTitle());
        profile.setCurrentCompany(candidate.getCurrentCompany());
        profile.setWorkYears(candidate.getWorkYears());
        profile.setEducation(candidate.getEducation());
        profile.setHasParsedResume(matchVerdictService.hasParsedResume(candidateId));

        return TagMatchScoreCalculator.calculate(jobTags, profile, objectMapper);
    }

    private String combineCandidateTags(String candidateTags, String resumeSkills) {
        if (!StringUtils.hasText(resumeSkills)) {
            return candidateTags;
        }
        if (!StringUtils.hasText(candidateTags)) {
            return resumeSkills;
        }
        return candidateTags + "," + resumeSkills;
    }

    private String extractResumeSkills(Long candidateId) {
        LambdaQueryWrapper<Resume> w = new LambdaQueryWrapper<>();
        w.eq(Resume::getCandidateId, candidateId)
                .in(Resume::getParseStatus, "SUCCESS", "PARSED", "2")
                .orderByDesc(Resume::getCreatedAt)
                .last("LIMIT 1");
        Resume resume = resumeMapper.selectOne(w);
        if (resume == null || !StringUtils.hasText(resume.getParsedJson())) {
            return null;
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(resume.getParsedJson(),
                    new TypeReference<Map<String, Object>>() {});
            Object skills = parsed.get("skills");
            if (skills instanceof List) {
                StringBuilder sb = new StringBuilder();
                for (Object item : (List<?>) skills) {
                    if (item != null) {
                        sb.append(item).append(',');
                    }
                }
                return sb.length() > 0 ? sb.toString() : null;
            }
        } catch (Exception ignored) {
            /* fall through */
        }
        return null;
    }

    /**
     * Convert Candidate entity to CandidateVO
     */
    private CandidateVO convertToVO(Candidate candidate) {
        CandidateVO vo = new CandidateVO();
        vo.setId(candidate.getId());
        vo.setName(candidate.getName());
        vo.setPhone(candidate.getPhone());
        vo.setEmail(candidate.getEmail());
        vo.setGender(candidate.getGender());
        vo.setBirthDate(candidate.getBirthDate());
        vo.setCurrentCompany(candidate.getCurrentCompany());
        vo.setCurrentTitle(candidate.getCurrentTitle());
        vo.setWorkYears(candidate.getWorkYears());
        vo.setEducation(candidate.getEducation());
        vo.setSchool(candidate.getSchool());
        vo.setMajor(candidate.getMajor());
        vo.setExpectedSalary(candidate.getExpectedSalary());
        vo.setWorkLocation(candidate.getWorkLocation());
        vo.setSource(candidate.getSource());
        vo.setSourceDetail(candidate.getSourceDetail());
        vo.setStatus(candidate.getStatus());
        vo.setTags(candidate.getTags());
        vo.setRemark(candidate.getRemark());
        vo.setCreatedAt(candidate.getCreatedAt());
        vo.setUpdatedAt(candidate.getUpdatedAt());
        vo.setResumeId(resolveLatestResumeId(candidate.getId()));
        return vo;
    }

    private Long resolveLatestResumeId(Long candidateId) {
        LambdaQueryWrapper<Resume> w = new LambdaQueryWrapper<>();
        w.eq(Resume::getCandidateId, candidateId)
                .orderByDesc(Resume::getCreatedAt)
                .last("LIMIT 1");
        Resume resume = resumeMapper.selectOne(w);
        return resume != null ? resume.getId() : null;
    }

    /**
     * Convert CandidateJob entity to CandidateJobVO
     */
    public CandidateJobVO convertToCandidateJobVO(CandidateJob job) {
        return convertToCandidateJobVO(job, false);
    }

    /**
     * @param refreshMatch 为 true 时按当前岗位样本重新计算匹配结论（仅写入 VO，不落库）
     */
    public CandidateJobVO convertToCandidateJobVO(CandidateJob job, boolean refreshMatch) {
        CandidateJobVO vo = new CandidateJobVO();
        vo.setId(job.getId());
        vo.setCandidateId(job.getCandidateId());
        vo.setJobId(job.getJobId());
        vo.setScreeningStatus(job.getScreeningStatus());
        vo.setPipelineStage(job.getPipelineStage());
        vo.setInterviewSubStage(job.getInterviewSubStage());
        vo.setRejectionReasonCode(job.getRejectionReasonCode());
        vo.setScreenerId(job.getScreenerId());
        vo.setScreenerComment(job.getScreenerComment());
        vo.setCreatedAt(job.getCreatedAt());
        vo.setUpdatedAt(job.getUpdatedAt());

        Candidate candidate = candidateMapper.selectById(job.getCandidateId());
        if (candidate != null) {
            vo.setCandidateName(candidate.getName());
            vo.setCandidatePhone(candidate.getPhone());
            vo.setCandidateEmail(candidate.getEmail());
            vo.setCandidateCompany(candidate.getCurrentCompany());
            vo.setCandidateTitle(candidate.getCurrentTitle());
        }

        if (refreshMatch) {
            Long tenantId = job.getTenantId() != null ? job.getTenantId() : TenantContext.getTenantId();
            BigDecimal score = job.getMatchScore() != null
                    ? job.getMatchScore()
                    : calculateMatchScore(job.getCandidateId(), job.getJobId());
            vo.setMatchScore(score);
            vo.setMatchDetail(matchVerdictService.buildMatchDetailJson(
                    tenantId, job.getCandidateId(), job.getJobId(), candidate, score));
        } else {
            vo.setMatchScore(job.getMatchScore());
            vo.setMatchDetail(job.getMatchDetail());
        }

        return vo;
    }
}
