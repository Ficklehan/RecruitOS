package com.recruitos.candidate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitos.candidate.entity.Candidate;
import com.recruitos.candidate.entity.CandidateJob;
import com.recruitos.candidate.entity.Resume;
import com.recruitos.candidate.mapper.CandidateJobMapper;
import com.recruitos.candidate.mapper.JobPositionReadMapper;
import com.recruitos.candidate.mapper.ResumeMapper;
import com.recruitos.common.match.MatchVerdictCandidateSnapshot;
import com.recruitos.common.match.MatchVerdictJsonBuilder;
import com.recruitos.common.match.MatchVerdictRankInfo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

/**
 * 匹配结论：保守档位 + 优劣势 + 岗位内相对位置
 */
@Service
public class MatchVerdictService {

    @Resource
    private CandidateJobMapper candidateJobMapper;
    @Resource
    private ResumeMapper resumeMapper;
    @Resource
    private JobPositionReadMapper jobPositionReadMapper;
    @Resource
    private ObjectMapper objectMapper;

    public String buildMatchDetailJson(Long tenantId, Long candidateId, Long jobId,
                                       Candidate candidate, BigDecimal totalScore) {
        MatchVerdictCandidateSnapshot snapshot = toSnapshot(candidate, candidateId);
        double score = totalScore != null ? totalScore.doubleValue() : 0;
        MatchVerdictRankInfo rank = computeRank(tenantId, jobId, score);
        String jobTags = jobId != null ? jobPositionReadMapper.selectTags(jobId, tenantId) : null;
        return MatchVerdictJsonBuilder.build(snapshot, totalScore, rank, jobTags, objectMapper);
    }

    public boolean hasParsedResume(Long candidateId) {
        LambdaQueryWrapper<Resume> w = new LambdaQueryWrapper<>();
        w.eq(Resume::getCandidateId, candidateId).eq(Resume::getParseStatus, "SUCCESS");
        return resumeMapper.selectCount(w) > 0;
    }

    private MatchVerdictCandidateSnapshot toSnapshot(Candidate candidate, Long candidateId) {
        MatchVerdictCandidateSnapshot snapshot = new MatchVerdictCandidateSnapshot();
        if (candidate != null) {
            snapshot.setEducation(candidate.getEducation());
            snapshot.setTags(candidate.getTags());
            snapshot.setCurrentCompany(candidate.getCurrentCompany());
            snapshot.setCurrentTitle(candidate.getCurrentTitle());
            snapshot.setWorkYears(candidate.getWorkYears());
        }
        snapshot.setHasParsedResume(hasParsedResume(candidateId));
        return snapshot;
    }

    private MatchVerdictRankInfo computeRank(Long tenantId, Long jobId, double score) {
        if (jobId == null) {
            return new MatchVerdictRankInfo();
        }
        LambdaQueryWrapper<CandidateJob> w = new LambdaQueryWrapper<>();
        w.eq(CandidateJob::getTenantId, tenantId).eq(CandidateJob::getJobId, jobId)
                .isNotNull(CandidateJob::getMatchScore)
                .orderByDesc(CandidateJob::getMatchScore);
        List<CandidateJob> list = candidateJobMapper.selectList(w);
        int higher = 0;
        for (CandidateJob cj : list) {
            if (cj.getMatchScore().doubleValue() > score) {
                higher++;
            }
        }
        return MatchVerdictJsonBuilder.computeRank(list.size(), higher);
    }
}
