package com.recruitos.candidate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruitos.candidate.dto.CandidateCreateDTO;
import com.recruitos.candidate.entity.Candidate;
import com.recruitos.candidate.entity.CandidateJob;
import com.recruitos.candidate.entity.Resume;
import com.recruitos.candidate.mapper.CandidateJobMapper;
import com.recruitos.candidate.mapper.CandidateMapper;
import com.recruitos.candidate.mapper.JobPositionReadMapper;
import com.recruitos.candidate.mapper.ResumeMapper;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.license.LicenseQuotaService;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ResumeService {

    @Resource
    private ResumeMapper resumeMapper;
    @Resource
    private CandidateMapper candidateMapper;
    @Resource
    private CandidateJobMapper candidateJobMapper;
    @Resource
    private JobPositionReadMapper jobPositionReadMapper;
    @Resource
    private CandidateService candidateService;
    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private LicenseQuotaService licenseQuotaService;

    public Map<String, Object> upload(MultipartFile file) {
        Long tenantId = TenantContext.getTenantId();
        Resume resume = new Resume();
        resume.setTenantId(tenantId);
        resume.setFileName(file.getOriginalFilename());
        resume.setFileUrl("/storage/uploads/" + UUID.randomUUID() + "-" + file.getOriginalFilename());
        resume.setFileType(extension(file.getOriginalFilename()));
        resume.setParseStatus("0");
        resume.setVersion(1);
        resume.setCreatedAt(LocalDateTime.now());
        resumeMapper.insert(resume);
        return toMap(resume);
    }

    public PageResult<Map<String, Object>> list(Integer pageNum, Integer pageSize, String parseStatus) {
        Long tenantId = TenantContext.getTenantId();
        Page<Resume> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20);
        LambdaQueryWrapper<Resume> w = new LambdaQueryWrapper<>();
        w.eq(Resume::getTenantId, tenantId);
        if (StringUtils.hasText(parseStatus)) {
            w.eq(Resume::getParseStatus, toDbParseStatus(parseStatus));
        }
        w.orderByDesc(Resume::getCreatedAt);
        Page<Resume> result = resumeMapper.selectPage(page, w);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Resume r : result.getRecords()) {
            rows.add(toMap(r));
        }
        return new PageResult<>(result.getTotal(), rows, (int) page.getCurrent(), (int) page.getSize());
    }

    public Map<String, Object> detail(Long id) {
        Resume resume = requireResume(id);
        return toMap(resume);
    }

    public Map<String, Object> parse(Long id) {
        Resume resume = requireResume(id);
        Long tenantId = resume.getTenantId();
        licenseQuotaService.assertCanParseResume(tenantId);
        resume.setParseStatus("1");
        resumeMapper.updateById(resume);
        String raw = resume.getRawText() != null ? resume.getRawText() : "候选人简历内容";
        Map<String, Object> parsed = new LinkedHashMap<>();
        Map<String, Object> basic = new LinkedHashMap<>();
        basic.put("name", "待确认候选人");
        basic.put("phone", "");
        basic.put("email", "");
        parsed.put("basic", basic);
        parsed.put("skills", Arrays.asList("Java", "Vue"));
        parsed.put("summary", raw);
        try {
            resume.setParsedJson(objectMapper.writeValueAsString(parsed));
        } catch (Exception e) {
            throw new BizException("解析结果序列化失败");
        }
        resume.setRawText(raw);
        resume.setParseStatus("2");
        resumeMapper.updateById(resume);
        licenseQuotaService.recordResumeParsed(tenantId);
        return toMap(resume);
    }

    public Map<String, Object> importToTalentPool(Long id) {
        Resume resume = requireResume(id);
        if (!"2".equals(resume.getParseStatus()) && !"SUCCESS".equalsIgnoreCase(String.valueOf(resume.getParseStatus()))) {
            parse(id);
            resume = requireResume(id);
        }
        CandidateCreateDTO dto = new CandidateCreateDTO();
        dto.setName("简历候选人");
        dto.setSource("RESUME");
        dto.setSourceDetail(resume.getFileName());
        com.recruitos.candidate.dto.CandidateVO vo = candidateService.createCandidate(dto);
        resume.setCandidateId(vo.getId());
        resumeMapper.updateById(resume);
        Map<String, Object> result = toMap(resume);
        result.put("candidateId", vo.getId());
        return result;
    }

    public void delete(Long id) {
        requireResume(id);
        resumeMapper.deleteById(id);
    }

    private Resume requireResume(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Resume resume = resumeMapper.selectById(id);
        if (resume == null || !tenantId.equals(resume.getTenantId())) {
            throw new BizException("简历不存在");
        }
        return resume;
    }

    private Map<String, Object> toMap(Resume r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("candidateId", r.getCandidateId());
        m.put("fileName", r.getFileName());
        m.put("fileUrl", r.getFileUrl());
        m.put("fileType", r.getFileType());
        m.put("parsedJson", r.getParsedJson());
        m.put("rawText", r.getRawText());
        m.put("parseStatus", normalizeParseStatus(r.getParseStatus()));
        m.put("version", r.getVersion());
        m.put("createdAt", r.getCreatedAt());
        enrichDisplayFields(m, r);
        return m;
    }

    /** Flatten parsed_json + candidate + match context for list/detail UI. */
    private void enrichDisplayFields(Map<String, Object> m, Resume r) {
        Map<String, Object> parsed = readParsedJson(r.getParsedJson());

        if (r.getCandidateId() != null) {
            Candidate c = candidateMapper.selectById(r.getCandidateId());
            if (c != null) {
                m.put("name", c.getName());
                m.put("phone", c.getPhone());
                m.put("email", c.getEmail());
                m.put("company", c.getCurrentCompany());
                m.put("position", c.getCurrentTitle());
                m.put("workYears", c.getWorkYears());
                m.put("education", normalizeEducation(c.getEducation()));
                m.put("source", mapCandidateSource(c.getSource()));
                if (c.getExpectedSalary() != null) {
                    int k = c.getExpectedSalary().intValue() >= 1000
                            ? c.getExpectedSalary().intValue() / 1000
                            : c.getExpectedSalary().intValue();
                    m.put("expectedSalary", k + "K");
                }
                m.put("school", c.getSchool());
                m.put("major", c.getMajor());
                m.put("workLocation", c.getWorkLocation());
                m.put("status", c.getStatus());
                attachMatchContext(m, c.getId());
            }
        }

        if (parsed != null) {
            applyParsedJson(m, parsed);
        }

        if (!m.containsKey("name")) {
            m.put("name", "未识别");
        }
    }

    @SuppressWarnings("unchecked")
    private void applyParsedJson(Map<String, Object> m, Map<String, Object> parsed) {
        Object basicObj = parsed.get("basic");
        if (basicObj instanceof Map) {
            Map<String, Object> basic = (Map<String, Object>) basicObj;
            putIfAbsent(m, "name", basic.get("name"));
            putIfAbsent(m, "phone", basic.get("phone"));
            putIfAbsent(m, "email", basic.get("email"));
            putIfAbsent(m, "company", basic.get("company"));
            putIfAbsent(m, "position", basic.get("position"));
            putIfAbsent(m, "workYears", basic.get("workYears"));
            putIfAbsent(m, "education", basic.get("education"));
            putIfAbsent(m, "expectedSalary", basic.get("expectedSalary"));
            putIfAbsent(m, "school", basic.get("school"));
            putIfAbsent(m, "major", basic.get("major"));
            putIfAbsent(m, "workLocation", basic.get("location"));
        }
        putIfAbsent(m, "skills", parsed.get("skills"));
        putIfAbsent(m, "summary", parsed.get("summary"));
        putIfAbsent(m, "workExperience", parsed.get("workExperience"));
        putIfAbsent(m, "projectExperience", parsed.get("projectExperience"));
        putIfAbsent(m, "educationHistory", parsed.get("education"));
        putIfAbsent(m, "aiInsights", parsed.get("aiInsights"));
    }

    private void attachMatchContext(Map<String, Object> m, Long candidateId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return;
        }
        LambdaQueryWrapper<CandidateJob> w = new LambdaQueryWrapper<>();
        w.eq(CandidateJob::getTenantId, tenantId)
                .eq(CandidateJob::getCandidateId, candidateId)
                .orderByDesc(CandidateJob::getMatchScore)
                .last("LIMIT 3");
        List<CandidateJob> jobs = candidateJobMapper.selectList(w);
        if (jobs.isEmpty()) {
            return;
        }
        CandidateJob top = jobs.get(0);
        m.put("matchScore", top.getMatchScore());
        m.put("matchDetail", top.getMatchDetail());

        List<Map<String, Object>> recommended = new ArrayList<>();
        for (CandidateJob cj : jobs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", cj.getJobId());
            item.put("title", jobPositionReadMapper.selectTitle(cj.getJobId(), tenantId));
            item.put("matchScore", cj.getMatchScore());
            item.put("matchDetail", cj.getMatchDetail());
            recommended.add(item);
        }
        m.put("recommendedJobs", recommended);
    }

    private void putIfAbsent(Map<String, Object> m, String key, Object value) {
        if (value != null && (!m.containsKey(key) || m.get(key) == null)) {
            m.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readParsedJson(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String normalizeEducation(String edu) {
        if (edu == null) return null;
        switch (edu.toUpperCase()) {
            case "MASTER": return "硕士";
            case "BACHELOR": return "本科";
            case "PHD": return "博士";
            case "COLLEGE": return "大专";
            default: return edu;
        }
    }

    private String mapCandidateSource(String source) {
        if (source == null) return "MANUAL";
        switch (source) {
            case "PLATFORM": return "BOSS";
            case "REFERRAL": return "REFERRAL";
            case "HEADHUNTER": return "HEADHUNTER";
            case "DIRECT": return "MANUAL";
            default: return source;
        }
    }

    private String normalizeParseStatus(String status) {
        if (status == null) {
            return "PENDING";
        }
        if ("0".equals(status) || "PENDING".equalsIgnoreCase(status)) {
            return "PENDING";
        }
        if ("1".equals(status) || "PARSING".equalsIgnoreCase(status)) {
            return "PARSING";
        }
        if ("2".equals(status) || "SUCCESS".equalsIgnoreCase(status) || "PARSED".equalsIgnoreCase(status)) {
            return "PARSED";
        }
        if ("3".equals(status) || "FAILED".equalsIgnoreCase(status)) {
            return "FAILED";
        }
        return status;
    }

    private String toDbParseStatus(String status) {
        if ("PARSED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) {
            return "2";
        }
        if ("FAILED".equalsIgnoreCase(status)) {
            return "3";
        }
        if ("PARSING".equalsIgnoreCase(status)) {
            return "1";
        }
        return "0";
    }

    private String extension(String name) {
        if (name == null || !name.contains(".")) {
            return "pdf";
        }
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase();
    }
}
