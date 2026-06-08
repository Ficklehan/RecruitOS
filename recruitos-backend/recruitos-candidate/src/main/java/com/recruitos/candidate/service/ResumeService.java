package com.recruitos.candidate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruitos.candidate.dto.CandidateCreateDTO;
import com.recruitos.candidate.entity.Candidate;
import com.recruitos.candidate.entity.Resume;
import com.recruitos.candidate.mapper.CandidateMapper;
import com.recruitos.candidate.mapper.ResumeMapper;
import com.recruitos.common.exception.BizException;
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
    private CandidateService candidateService;
    @Resource
    private ObjectMapper objectMapper;

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

    /** Flatten parsed_json / linked candidate for list UI. */
    private void enrichDisplayFields(Map<String, Object> m, Resume r) {
        if (r.getCandidateId() != null) {
            Candidate c = candidateMapper.selectById(r.getCandidateId());
            if (c != null) {
                m.put("name", c.getName());
                m.put("phone", c.getPhone());
                m.put("company", c.getCurrentCompany());
                m.put("position", c.getCurrentTitle());
                m.put("workYears", c.getWorkYears());
                m.put("education", normalizeEducation(c.getEducation()));
                m.put("source", mapCandidateSource(c.getSource()));
            }
            return;
        }
        if (!StringUtils.hasText(r.getParsedJson())) {
            m.put("name", "未识别");
            return;
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(r.getParsedJson(), Map.class);
            Object basicObj = parsed.get("basic");
            if (basicObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> basic = (Map<String, Object>) basicObj;
                m.put("name", basic.getOrDefault("name", "未识别"));
                m.put("phone", basic.get("phone"));
                m.put("company", basic.get("company"));
                m.put("position", basic.get("position"));
                m.put("workYears", basic.get("workYears"));
                m.put("education", basic.get("education"));
            }
            m.put("source", "MANUAL");
        } catch (Exception ignored) {
            m.put("name", "未识别");
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
