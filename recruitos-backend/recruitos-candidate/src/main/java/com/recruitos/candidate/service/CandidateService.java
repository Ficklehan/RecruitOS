package com.recruitos.candidate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruitos.candidate.dto.*;
import com.recruitos.candidate.entity.Candidate;
import com.recruitos.candidate.entity.CandidateJob;
import com.recruitos.candidate.entity.Resume;
import com.recruitos.candidate.mapper.CandidateJobMapper;
import com.recruitos.candidate.mapper.CandidateMapper;
import com.recruitos.candidate.mapper.ResumeMapper;
import com.recruitos.common.exception.BizException;
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
            jobVOs.add(convertToCandidateJobVO(job));
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

        List<CandidateVO> voList = new ArrayList<>();
        for (Candidate candidate : result.getRecords()) {
            voList.add(convertToVO(candidate));
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

        // Check if already associated
        LambdaQueryWrapper<CandidateJob> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(CandidateJob::getTenantId, tenantId)
                .eq(CandidateJob::getCandidateId, candidateId)
                .eq(CandidateJob::getJobId, jobId);
        Long existCount = candidateJobMapper.selectCount(existWrapper);
        if (existCount > 0) {
            throw new BizException("Candidate is already associated with this job");
        }

        // Calculate match score (simulated)
        BigDecimal matchScore = calculateMatchScore(candidateId, jobId);
        String matchDetail = buildMatchDetail(candidateId, jobId, matchScore);

        CandidateJob candidateJob = new CandidateJob();
        candidateJob.setCandidateId(candidateId);
        candidateJob.setJobId(jobId);
        candidateJob.setMatchScore(matchScore);
        candidateJob.setMatchDetail(matchDetail);
        candidateJob.setScreeningStatus("PENDING");

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

        CandidateJobVO vo = convertToCandidateJobVO(candidateJob);

        // Load candidate info for the panel
        Candidate candidate = candidateMapper.selectById(candidateId);
        if (candidate != null) {
            vo.setCandidateName(candidate.getName());
            vo.setCandidatePhone(candidate.getPhone());
            vo.setCandidateEmail(candidate.getEmail());
            vo.setCandidateCompany(candidate.getCurrentCompany());
            vo.setCandidateTitle(candidate.getCurrentTitle());
        }

        return vo;
    }

    /**
     * Talent pool global search by keyword and tags
     */
    public PageResult<CandidateVO> getTalentPool(String keyword, String tags) {
        Long tenantId = TenantContext.getTenantId();

        Page<Candidate> page = new Page<>(1, 50);

        LambdaQueryWrapper<Candidate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Candidate::getTenantId, tenantId);

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
            voList.add(convertToVO(candidate));
        }

        return new PageResult<>(result.getTotal(), voList, 1, 50);
    }

    /**
     * Simulated match score calculation.
     * In a real system this would use AI/embedding matching.
     *
     * Logic:
     * 1. Get job tag weights
     * 2. Get candidate resume parsed skill tags
     * 3. Calculate match per tag
     * 4. Weighted sum to get total score
     * 5. Save to candidate_job table
     */
    private BigDecimal calculateMatchScore(Long candidateId, Long jobId) {
        Candidate candidate = candidateMapper.selectById(candidateId);
        if (candidate == null) {
            return BigDecimal.ZERO;
        }

        // Simulated: use candidate fields to compute a score
        double score = 50.0; // base score

        // Bonus for work years
        if (candidate.getWorkYears() != null && candidate.getWorkYears() >= 3) {
            score += 10.0;
        }
        if (candidate.getWorkYears() != null && candidate.getWorkYears() >= 5) {
            score += 5.0;
        }

        // Bonus for education
        if (StringUtils.hasText(candidate.getEducation())) {
            String edu = candidate.getEducation();
            if (edu.contains("硕士") || edu.contains("Master")) {
                score += 10.0;
            } else if (edu.contains("博士") || edu.contains("PhD")) {
                score += 15.0;
            } else if (edu.contains("本科") || edu.contains("Bachelor")) {
                score += 5.0;
            }
        }

        // Bonus for having a resume parsed
        LambdaQueryWrapper<Resume> resumeWrapper = new LambdaQueryWrapper<>();
        resumeWrapper.eq(Resume::getCandidateId, candidateId)
                .eq(Resume::getParseStatus, "SUCCESS");
        Long resumeCount = resumeMapper.selectCount(resumeWrapper);
        if (resumeCount > 0) {
            score += 10.0;
        }

        // Cap at 100
        if (score > 100.0) {
            score = 100.0;
        }

        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Build match detail JSON (simulated breakdown)
     */
    private String buildMatchDetail(Long candidateId, Long jobId, BigDecimal totalScore) {
        Candidate candidate = candidateMapper.selectById(candidateId);
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"totalScore\":").append(totalScore).append(",");
        sb.append("\"breakdown\":{");

        // Skill match
        double skillScore = 40.0;
        if (candidate != null && StringUtils.hasText(candidate.getTags())) {
            skillScore = 60.0;
        }
        sb.append("\"skillMatch\":").append(skillScore).append(",");

        // Experience match
        double expScore = 30.0;
        if (candidate != null && candidate.getWorkYears() != null && candidate.getWorkYears() >= 3) {
            expScore = 50.0;
        }
        sb.append("\"experienceMatch\":").append(expScore).append(",");

        // Education match
        double eduScore = 20.0;
        if (candidate != null && StringUtils.hasText(candidate.getEducation())) {
            eduScore = 40.0;
        }
        sb.append("\"educationMatch\":").append(eduScore);

        sb.append("}}");
        return sb.toString();
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
        return vo;
    }

    /**
     * Convert CandidateJob entity to CandidateJobVO
     */
    private CandidateJobVO convertToCandidateJobVO(CandidateJob job) {
        CandidateJobVO vo = new CandidateJobVO();
        vo.setId(job.getId());
        vo.setCandidateId(job.getCandidateId());
        vo.setJobId(job.getJobId());
        vo.setMatchScore(job.getMatchScore());
        vo.setMatchDetail(job.getMatchDetail());
        vo.setScreeningStatus(job.getScreeningStatus());
        vo.setScreenerId(job.getScreenerId());
        vo.setScreenerComment(job.getScreenerComment());
        vo.setCreatedAt(job.getCreatedAt());
        vo.setUpdatedAt(job.getUpdatedAt());

        // Load candidate info
        Candidate candidate = candidateMapper.selectById(job.getCandidateId());
        if (candidate != null) {
            vo.setCandidateName(candidate.getName());
            vo.setCandidatePhone(candidate.getPhone());
            vo.setCandidateEmail(candidate.getEmail());
            vo.setCandidateCompany(candidate.getCurrentCompany());
            vo.setCandidateTitle(candidate.getCurrentTitle());
        }

        return vo;
    }
}
