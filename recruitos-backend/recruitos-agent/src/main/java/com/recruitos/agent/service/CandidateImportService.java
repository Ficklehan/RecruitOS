package com.recruitos.agent.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitos.agent.mapper.CandidateImportMapper;
import com.recruitos.agent.platform.PlatformCandidate;
import com.recruitos.agent.platform.PlatformResume;
import com.recruitos.common.match.MatchVerdictCandidateSnapshot;
import com.recruitos.common.match.MatchVerdictJsonBuilder;
import com.recruitos.common.match.MatchVerdictRankInfo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;

@Service
public class CandidateImportService {

    @Resource
    private CandidateImportMapper candidateImportMapper;
    @Resource
    private ObjectMapper objectMapper;

    public Long findExistingCandidateId(Long tenantId, String phone, String email) {
        if (phone != null && !phone.isEmpty()) {
            Long id = candidateImportMapper.findCandidateIdByPhone(tenantId, phone);
            if (id != null) {
                return id;
            }
        }
        if (email != null && !email.isEmpty()) {
            return candidateImportMapper.findCandidateIdByEmail(tenantId, email);
        }
        return null;
    }

    public ImportResult importCandidate(Long tenantId, Long jobId, PlatformCandidate pc, PlatformResume resume, BigDecimal matchScore) {
        Long candidateId = findExistingCandidateId(tenantId, pc.getPhone(), pc.getEmail());
        boolean created = false;
        if (candidateId == null) {
            candidateId = IdWorker.getId();
            candidateImportMapper.insertCandidate(
                    candidateId, tenantId, pc.getName(), pc.getPhone(), pc.getEmail(),
                    pc.getCompany(), pc.getTitle(), pc.getWorkYears(),
                    "AGENT:" + pc.getPlatformUserId());
            created = true;
        }

        boolean hasParsedResume = false;
        Long resumeId = null;
        if (resume != null) {
            resumeId = IdWorker.getId();
            hasParsedResume = StringUtils.hasText(resume.getParsedJson());
            String parseStatus = hasParsedResume ? "SUCCESS" : "PENDING";
            candidateImportMapper.insertResume(
                    resumeId, tenantId, candidateId,
                    resume.getFileName(), resume.getFileUrl(),
                    resume.getParsedJson(), resume.getRawText(), parseStatus);
        }

        Long candidateJobId = null;
        if (candidateImportMapper.countCandidateJob(tenantId, candidateId, jobId) == 0) {
            candidateJobId = IdWorker.getId();
            BigDecimal score = matchScore != null ? matchScore : pc.getMatchScore();
            String matchDetail = buildAgentMatchDetail(tenantId, jobId, pc, score, hasParsedResume);
            candidateImportMapper.insertCandidateJob(
                    candidateJobId, tenantId, candidateId, jobId, score, matchDetail);
        }

        ImportResult result = new ImportResult();
        result.setCandidateId(candidateId);
        result.setResumeId(resumeId);
        result.setCandidateJobId(candidateJobId);
        result.setNewCandidate(created);
        return result;
    }

    /**
     * 为寻源轨迹等场景构建匹配结论 JSON（候选人可能尚未入库）
     */
    public String buildTraceMatchDetail(Long tenantId, Long jobId, PlatformCandidate pc,
                                        BigDecimal matchScore, boolean hasParsedResume) {
        return buildAgentMatchDetail(tenantId, jobId, pc, matchScore, hasParsedResume);
    }

    private String buildAgentMatchDetail(Long tenantId, Long jobId, PlatformCandidate pc,
                                         BigDecimal matchScore, boolean hasParsedResume) {
        MatchVerdictCandidateSnapshot snapshot = new MatchVerdictCandidateSnapshot();
        snapshot.setCurrentCompany(pc.getCompany());
        snapshot.setCurrentTitle(pc.getTitle());
        snapshot.setWorkYears(pc.getWorkYears());
        snapshot.setHasParsedResume(hasParsedResume);

        double score = matchScore != null ? matchScore.doubleValue() : 0;
        int total = candidateImportMapper.countJobRanked(tenantId, jobId);
        int higher = candidateImportMapper.countHigherScores(tenantId, jobId, score);
        MatchVerdictRankInfo rank = MatchVerdictJsonBuilder.computeRank(total, higher);

        String jobTags = candidateImportMapper.selectJobTags(jobId, tenantId);
        return MatchVerdictJsonBuilder.build(snapshot, matchScore, rank, jobTags, objectMapper);
    }

    public static class ImportResult {
        private Long candidateId;
        private Long resumeId;
        private Long candidateJobId;
        private boolean newCandidate;

        public Long getCandidateId() { return candidateId; }
        public void setCandidateId(Long candidateId) { this.candidateId = candidateId; }
        public Long getResumeId() { return resumeId; }
        public void setResumeId(Long resumeId) { this.resumeId = resumeId; }
        public Long getCandidateJobId() { return candidateJobId; }
        public void setCandidateJobId(Long candidateJobId) { this.candidateJobId = candidateJobId; }
        public boolean isNewCandidate() { return newCandidate; }
        public void setNewCandidate(boolean newCandidate) { this.newCandidate = newCandidate; }
    }
}
