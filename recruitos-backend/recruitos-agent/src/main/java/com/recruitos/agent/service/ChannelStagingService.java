package com.recruitos.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitos.agent.dto.ChannelStagingQueryDTO;
import com.recruitos.agent.dto.ChannelStagingVO;
import com.recruitos.agent.entity.CampaignCandidateTrace;
import com.recruitos.agent.entity.ChannelStagingCandidate;
import com.recruitos.agent.mapper.CampaignCandidateTraceMapper;
import com.recruitos.agent.mapper.CandidateImportMapper;
import com.recruitos.agent.mapper.ChannelStagingCandidateMapper;
import com.recruitos.agent.platform.PlatformCandidate;
import com.recruitos.agent.platform.PlatformResume;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChannelStagingService {

    private static final List<String> ACTIVE_STATUSES = Arrays.asList("STAGED", "COLLECTED", "GREETED");

    @Resource
    private ChannelStagingCandidateMapper stagingMapper;
    @Resource
    private CampaignCandidateTraceMapper traceMapper;
    @Resource
    private CandidateImportMapper jobReadMapper;
    @Resource
    private CandidateImportService importService;
    @Resource
    private CampaignOrchestratorService orchestratorService;
    @Resource
    private CampaignEvolutionEmitter evolutionEmitter;
    @Resource
    private ObjectMapper objectMapper;

    public PageResult<ChannelStagingVO> list(ChannelStagingQueryDTO query) {
        Long tenantId = TenantContext.getTenantId();
        Page<ChannelStagingCandidate> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<ChannelStagingCandidate> w = new LambdaQueryWrapper<>();
        w.eq(ChannelStagingCandidate::getTenantId, tenantId);
        if (query.getJobId() != null) {
            w.eq(ChannelStagingCandidate::getJobId, query.getJobId());
        }
        if (StringUtils.hasText(query.getPlatform())) {
            w.eq(ChannelStagingCandidate::getPlatform, query.getPlatform());
        }
        if (StringUtils.hasText(query.getStatus())) {
            w.eq(ChannelStagingCandidate::getStatus, query.getStatus());
        } else {
            w.in(ChannelStagingCandidate::getStatus, ACTIVE_STATUSES);
        }
        applySort(w, query.getSort());
        Page<ChannelStagingCandidate> result = stagingMapper.selectPage(page, w);
        List<ChannelStagingVO> list = new ArrayList<>();
        for (ChannelStagingCandidate row : result.getRecords()) {
            list.add(toVO(row, tenantId));
        }
        return new PageResult<>(result.getTotal(), list, query.getPageNum(), query.getPageSize());
    }

    public ChannelStagingVO getById(Long id) {
        ChannelStagingCandidate row = requireStaging(id);
        return toVO(row, row.getTenantId());
    }

    @Transactional
    public ChannelStagingVO updateExtractedFields(Long id, Map<String, Object> fields) {
        ChannelStagingCandidate row = requireStaging(id);
        try {
            Map<String, Object> merged = parseFields(row.getExtractedFieldsJson());
            if (fields != null) {
                merged.putAll(fields);
            }
            row.setExtractedFieldsJson(objectMapper.writeValueAsString(merged));
            row.setUpdatedAt(LocalDateTime.now());
            stagingMapper.updateById(row);
        } catch (Exception e) {
            throw new BizException("字段更新失败");
        }
        return toVO(row, row.getTenantId());
    }

    @Transactional
    public Map<String, Object> askAi(Long id, String question) {
        ChannelStagingCandidate row = requireStaging(id);
        String answer = extractAnswer(row, question);
        Map<String, Object> fields = parseFields(row.getExtractedFieldsJson());
        fields.put("_lastQuestion", question);
        fields.put("_lastAnswer", answer);
        fields.put("aiAnswerAt", LocalDateTime.now().toString());
        updateExtractedFields(id, fields);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("question", question);
        resp.put("answer", answer);
        resp.put("extractedFields", fields);
        return resp;
    }

    @Transactional
    public Map<String, Object> batchGreet(List<Long> ids) {
        int ok = 0;
        for (Long id : ids) {
            ChannelStagingCandidate row = requireStaging(id);
            if (!isActionable(row)) {
                continue;
            }
            if (row.getTraceId() != null) {
                orchestratorService.confirmGreet(row.getTraceId());
            }
            row.setStatus("GREETED");
            row.setUpdatedAt(LocalDateTime.now());
            stagingMapper.updateById(row);
            ok++;
        }
        return resultMap(ok, ids.size());
    }

    @Transactional
    public Map<String, Object> batchImport(List<Long> ids) {
        Long tenantId = TenantContext.getTenantId();
        int ok = 0;
        for (Long id : ids) {
            ChannelStagingCandidate row = requireStaging(id);
            if ("IMPORTED".equals(row.getStatus())) {
                continue;
            }
            PlatformCandidate pc = toPlatformCandidate(row);
            if (row.getTraceId() != null) {
                CampaignCandidateTrace trace = traceMapper.selectById(row.getTraceId());
                if (trace != null) {
                    pc.setPhone(trace.getPhone());
                    pc.setEmail(trace.getEmail());
                }
            }
            PlatformResume resume = buildResume(row);
            CandidateImportService.ImportResult result = importService.importCandidate(
                    tenantId, row.getJobId(), pc, resume, row.getMatchScore());
            row.setCandidateId(result.getCandidateId());
            row.setStatus("IMPORTED");
            row.setUpdatedAt(LocalDateTime.now());
            stagingMapper.updateById(row);
            if (row.getTraceId() != null) {
                CampaignCandidateTrace trace = traceMapper.selectById(row.getTraceId());
                if (trace != null) {
                    trace.setCandidateId(result.getCandidateId());
                    trace.setResumeId(result.getResumeId());
                    trace.setTraceStatus("IMPORTED");
                    trace.setUpdatedAt(LocalDateTime.now());
                    traceMapper.updateById(trace);
                }
            }
            evolutionEmitter.emitScreen(row.getJobId(), row.getCampaignId(), row.getTraceId(),
                    result.getCandidateId(), true, tagAdjust("_import", 0.1));
            ok++;
        }
        return resultMap(ok, ids.size());
    }

    @Transactional
    public Map<String, Object> batchReject(List<Long> ids, String reason) {
        int ok = 0;
        for (Long id : ids) {
            ChannelStagingCandidate row = requireStaging(id);
            if ("IMPORTED".equals(row.getStatus()) || "REJECTED".equals(row.getStatus())) {
                continue;
            }
            row.setStatus("REJECTED");
            row.setUpdatedAt(LocalDateTime.now());
            Map<String, Object> fields = parseFields(row.getExtractedFieldsJson());
            fields.put("rejectReason", StringUtils.hasText(reason) ? reason : "HR标记不合适");
            try {
                row.setExtractedFieldsJson(objectMapper.writeValueAsString(fields));
            } catch (Exception ignored) {
            }
            stagingMapper.updateById(row);
            evolutionEmitter.emitScreen(row.getJobId(), row.getCampaignId(), row.getTraceId(),
                    null, false, tagAdjust("_reject", -0.08));
            ok++;
        }
        return resultMap(ok, ids.size());
    }

    private boolean isActionable(ChannelStagingCandidate row) {
        return ACTIVE_STATUSES.contains(row.getStatus());
    }

    private PlatformCandidate toPlatformCandidate(ChannelStagingCandidate row) {
        PlatformCandidate pc = new PlatformCandidate();
        pc.setPlatformUserId(row.getPlatformUserId());
        pc.setName(row.getCandidateName());
        pc.setMatchScore(row.getMatchScore());
        Map<String, Object> fields = parseFields(row.getExtractedFieldsJson());
        if (fields.get("company") != null) {
            pc.setCompany(fields.get("company").toString());
        }
        if (fields.get("title") != null) {
            pc.setTitle(fields.get("title").toString());
        }
        return pc;
    }

    private PlatformResume buildResume(ChannelStagingCandidate row) {
        PlatformResume resume = new PlatformResume();
        resume.setFileName(row.getCandidateName() + "-渠道暂存.txt");
        resume.setRawText(StringUtils.hasText(row.getResumeText())
                ? row.getResumeText() : row.getCandidateName() + " 渠道暂存简历");
        Map<String, Object> fields = parseFields(row.getExtractedFieldsJson());
        try {
            resume.setParsedJson(objectMapper.writeValueAsString(fields));
        } catch (Exception e) {
            resume.setParsedJson("{}");
        }
        return resume;
    }

    private String extractAnswer(ChannelStagingCandidate row, String question) {
        String text = StringUtils.hasText(row.getResumeText()) ? row.getResumeText() : "";
        Map<String, Object> fields = parseFields(row.getExtractedFieldsJson());
        String q = question != null ? question.toLowerCase() : "";
        if (q.contains("公司") || q.contains("上家")) {
            if (fields.get("company") != null) {
                return "上一家公司：" + fields.get("company");
            }
            if (text.contains("公司")) {
                return "根据简历片段，候选人提及相关公司经历（详见简历文本）";
            }
        }
        if (q.contains("学历") || q.contains("院校")) {
            if (fields.get("school") != null) {
                return "毕业院校：" + fields.get("school");
            }
        }
        if (q.contains("年限") || q.contains("经验")) {
            if (fields.get("workYears") != null) {
                return "工作年限：" + fields.get("workYears");
            }
        }
        if (StringUtils.hasText(text)) {
            return text.length() > 120 ? text.substring(0, 120) + "…" : text;
        }
        return "暂无可提取信息，请补充简历文本或自定义字段";
    }

    private Map<String, Object> tagAdjust(String key, double val) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(key, val);
        return m;
    }

    private Map<String, Object> resultMap(int ok, int total) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("successCount", ok);
        m.put("total", total);
        return m;
    }

    private void applySort(LambdaQueryWrapper<ChannelStagingCandidate> w, String sort) {
        if ("createdAt".equalsIgnoreCase(sort)) {
            w.orderByDesc(ChannelStagingCandidate::getCreatedAt);
        } else {
            w.orderByDesc(ChannelStagingCandidate::getMatchScore);
        }
    }

    private ChannelStagingCandidate requireStaging(Long id) {
        Long tenantId = TenantContext.getTenantId();
        ChannelStagingCandidate row = stagingMapper.selectById(id);
        if (row == null || !row.getTenantId().equals(tenantId)) {
            throw new BizException("暂存记录不存在");
        }
        return row;
    }

    private Map<String, Object> parseFields(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private ChannelStagingVO toVO(ChannelStagingCandidate row, Long tenantId) {
        ChannelStagingVO vo = new ChannelStagingVO();
        vo.setId(row.getId());
        vo.setJobId(row.getJobId());
        vo.setJobTitle(jobReadMapper.selectJobTitle(row.getJobId(), tenantId));
        vo.setCampaignId(row.getCampaignId());
        vo.setTraceId(row.getTraceId());
        vo.setPlatform(row.getPlatform());
        vo.setPlatformUserId(row.getPlatformUserId());
        vo.setCandidateName(row.getCandidateName());
        vo.setMatchScore(row.getMatchScore());
        vo.setStatus(normalizeStatus(row.getStatus()));
        vo.setScreenshotUrl(row.getScreenshotUrl());
        vo.setExtractedFields(parseFields(row.getExtractedFieldsJson()));
        vo.setResumeText(row.getResumeText());
        vo.setCandidateId(row.getCandidateId());
        vo.setCreatedAt(row.getCreatedAt());
        vo.setUpdatedAt(row.getUpdatedAt());
        return vo;
    }

    private String normalizeStatus(String status) {
        return "COLLECTED".equals(status) ? "STAGED" : status;
    }
}
