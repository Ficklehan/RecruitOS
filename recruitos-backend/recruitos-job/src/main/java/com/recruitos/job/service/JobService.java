package com.recruitos.job.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitos.common.auth.CurrentUser;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.license.LicenseQuotaService;
import com.recruitos.common.llm.LlmChatRequest;
import com.recruitos.common.llm.LlmClient;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.job.dto.*;
import com.recruitos.job.entity.JobPosition;
import com.recruitos.job.mapper.JobPositionMapper;
import com.recruitos.job.mapper.RecruitDemandReadMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Job position service - core business logic for job management
 */
@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    @Resource
    private JobPositionMapper jobPositionMapper;

    @Resource
    private RecruitDemandReadMapper recruitDemandReadMapper;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private LicenseQuotaService licenseQuotaService;

    @Resource
    private LlmClient llmClient;

    private static final java.util.Set<String> ALLOWED_DEMAND_STATUS = new java.util.HashSet<>(
            java.util.Arrays.asList("APPROVED", "JOB_CREATED", "RECRUITING", "COMPLETED")
    );

    /** Sequence counter for job number generation (in-memory, resets on restart) */
    private final AtomicLong jobSeqCounter = new AtomicLong(0);

    /** Predefined tech stack keywords for JD parsing */
    private static final List<String> TECH_KEYWORDS = Arrays.asList(
            // Programming Languages
            "Java", "Python", "Go", "JavaScript", "TypeScript", "React", "Vue", "Angular",
            // Frameworks
            "Spring", "Spring Boot", "MyBatis", "Django", "Flask", "Express",
            // Databases
            "MySQL", "PostgreSQL", "MongoDB", "Redis", "Elasticsearch",
            // DevOps
            "Docker", "Kubernetes", "Jenkins", "GitLab CI", "Terraform",
            // Cloud
            "AWS", "Azure", "GCP", "阿里云", "腾讯云",
            // Architecture & Concepts
            "微服务", "分布式", "高并发", "高可用", "系统设计"
    );

    /**
     * Create a new job position
     */
    @Transactional
    public JobVO createJob(JobCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = CurrentUser.getCurrentUserId();
        if (tenantId == null || userId == null) {
            throw new BizException("User not authenticated");
        }

        validateDemandForJob(dto.getDemandId(), tenantId);
        licenseQuotaService.assertCanCreateJob(tenantId);

        JobPosition job = new JobPosition();
        job.setTenantId(tenantId);
        job.setDemandId(dto.getDemandId());
        job.setJobNo(generateJobNo());
        job.setTitle(dto.getTitle());
        job.setJdText(dto.getJdText());
        job.setHeadCount(dto.getHeadCount());
        job.setFilledCount(0);
        job.setStatus("DRAFT");

        jobPositionMapper.insert(job);
        licenseQuotaService.recordJobCreated(tenantId);

        return convertToVO(job);
    }

    /**
     * Update a job position
     */
    @Transactional
    public JobVO updateJob(Long id, JobUpdateDTO dto) {
        Long tenantId = TenantContext.getTenantId();

        JobPosition job = jobPositionMapper.selectById(id);
        if (job == null) {
            throw new BizException("Job position not found");
        }
        if (!job.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        if (StringUtils.hasText(dto.getTitle())) {
            job.setTitle(dto.getTitle());
        }
        if (dto.getJdText() != null) {
            job.setJdText(dto.getJdText());
        }
        if (dto.getHeadCount() != null) {
            job.setHeadCount(dto.getHeadCount());
        }

        jobPositionMapper.updateById(job);

        return convertToVO(job);
    }

    /**
     * Get job detail by ID
     */
    public JobVO getJobDetail(Long id) {
        Long tenantId = TenantContext.getTenantId();

        JobPosition job = jobPositionMapper.selectById(id);
        if (job == null) {
            throw new BizException("Job position not found");
        }
        if (!job.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        return convertToVO(job);
    }

    /**
     * List jobs with pagination and filters
     */
    public PageResult<JobVO> listJobs(JobQueryDTO query) {
        Long tenantId = TenantContext.getTenantId();

        Page<JobPosition> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<JobPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobPosition::getTenantId, tenantId);

        if (StringUtils.hasText(query.getTitle())) {
            wrapper.like(JobPosition::getTitle, query.getTitle());
        }
        if (query.getDemandId() != null) {
            wrapper.eq(JobPosition::getDemandId, query.getDemandId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(JobPosition::getStatus, query.getStatus());
        }

        wrapper.orderByDesc(JobPosition::getCreatedAt);

        Page<JobPosition> result = jobPositionMapper.selectPage(page, wrapper);

        List<JobVO> voList = new ArrayList<>();
        for (JobPosition job : result.getRecords()) {
            voList.add(convertToVO(job));
        }

        return new PageResult<>(result.getTotal(), voList, query.getPageNum(), query.getPageSize());
    }

    /**
     * Activate a job position (DRAFT -> ACTIVE)
     */
    @Transactional
    public JobVO activateJob(Long id) {
        Long tenantId = TenantContext.getTenantId();

        JobPosition job = jobPositionMapper.selectById(id);
        if (job == null) {
            throw new BizException("Job position not found");
        }
        if (!job.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if (!"DRAFT".equals(job.getStatus())) {
            throw new BizException("Only DRAFT jobs can be activated");
        }

        validateDemandForJob(job.getDemandId(), tenantId);

        job.setStatus("ACTIVE");
        jobPositionMapper.updateById(job);

        recruitDemandReadMapper.updateStatus(job.getDemandId(), tenantId, "RECRUITING");

        return convertToVO(job);
    }

    /**
     * Pause a job position (ACTIVE -> PAUSED)
     */
    @Transactional
    public JobVO pauseJob(Long id) {
        Long tenantId = TenantContext.getTenantId();

        JobPosition job = jobPositionMapper.selectById(id);
        if (job == null) {
            throw new BizException("Job position not found");
        }
        if (!job.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if (!"ACTIVE".equals(job.getStatus())) {
            throw new BizException("Only ACTIVE jobs can be paused");
        }

        job.setStatus("PAUSED");
        jobPositionMapper.updateById(job);

        return convertToVO(job);
    }

    /**
     * Close a job position (-> CLOSED, return unused head count)
     */
    @Transactional
    public JobVO closeJob(Long id, String reason) {
        Long tenantId = TenantContext.getTenantId();

        JobPosition job = jobPositionMapper.selectById(id);
        if (job == null) {
            throw new BizException("Job position not found");
        }
        if (!job.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if ("CLOSED".equals(job.getStatus())) {
            throw new BizException("Job position is already closed");
        }

        int unusedHeadcount = Math.max(0, job.getHeadCount() - (job.getFilledCount() != null ? job.getFilledCount() : 0));

        job.setStatus("CLOSED");
        job.setClosedReason(reason);
        jobPositionMapper.updateById(job);

        if (unusedHeadcount > 0 && job.getDemandId() != null) {
            Integer approved = recruitDemandReadMapper.selectApprovedHeadCount(job.getDemandId(), tenantId);
            if (approved != null && approved > 0) {
                recruitDemandReadMapper.updateStatus(job.getDemandId(), tenantId, "RECRUITING");
            }
        }

        return convertToVO(job);
    }

    private void validateDemandForJob(Long demandId, Long tenantId) {
        if (demandId == null) {
            throw new BizException("Demand ID is required");
        }
        String status = recruitDemandReadMapper.selectStatus(demandId, tenantId);
        if (status == null) {
            throw new BizException("Associated demand not found");
        }
        if (!ALLOWED_DEMAND_STATUS.contains(status)) {
            throw new BizException("Cannot create or activate job: demand is not approved");
        }
    }

    /**
     * Parse JD text (simulated LLM parsing) - extracts tags with tri-weights
     */
    @Transactional
    public JdParseResultVO parseJd(Long id) {
        Long tenantId = TenantContext.getTenantId();

        JobPosition job = jobPositionMapper.selectById(id);
        if (job == null) {
            throw new BizException("Job position not found");
        }
        if (!job.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if (!StringUtils.hasText(job.getJdText())) {
            throw new BizException("JD text is empty, cannot parse");
        }

        // LLM 解析（MiMo），失败时回退规则引擎
        JdParseResultVO result = parseJdWithLlm(job.getJdText());

        // Persist parsed tags to the job
        try {
            String tagsJson = objectMapper.writeValueAsString(result.getTags());
            job.setTags(tagsJson);
            String parsedJson = objectMapper.writeValueAsString(result);
            job.setJdParsedJson(parsedJson);
        } catch (JsonProcessingException e) {
            throw new BizException("Failed to serialize parse result");
        }
        jobPositionMapper.updateById(job);

        return result;
    }

    /**
     * Update tags for a job position
     */
    @Transactional
    public List<TagDTO> updateTags(Long id, List<TagDTO> tagDTOs) {
        Long tenantId = TenantContext.getTenantId();

        JobPosition job = jobPositionMapper.selectById(id);
        if (job == null) {
            throw new BizException("Job position not found");
        }
        if (!job.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        try {
            String tagsJson = objectMapper.writeValueAsString(tagDTOs);
            job.setTags(tagsJson);
        } catch (JsonProcessingException e) {
            throw new BizException("Failed to serialize tags");
        }
        jobPositionMapper.updateById(job);

        return tagDTOs;
    }

    /**
     * Get tags for a job position
     */
    public List<TagDTO> getTags(Long id) {
        Long tenantId = TenantContext.getTenantId();

        JobPosition job = jobPositionMapper.selectById(id);
        if (job == null) {
            throw new BizException("Job position not found");
        }
        if (!job.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        if (!StringUtils.hasText(job.getTags())) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(job.getTags(), new TypeReference<List<TagDTO>>() {});
        } catch (JsonProcessingException e) {
            throw new BizException("Failed to parse tags JSON");
        }
    }

    /**
     * 使用 MiMo LLM 解析 JD；失败时回退 simulateJdParsing。
     */
    private JdParseResultVO parseJdWithLlm(String jdText) {
        LlmChatRequest req = new LlmChatRequest();
        req.setScenario("jd_parse");
        req.setSystemPrompt(
                "你是招聘 JD 解析专家。从岗位描述中提取技能/要求标签，输出严格 JSON，不要 markdown。"
                        + "格式: {\"tags\":[{\"tag\":\"Java\",\"matchWeight\":0.9,\"searchWeight\":0.85,"
                        + "\"decisionWeight\":0.7,\"locked\":false}],"
                        + "\"entities\":{\"skills\":[\"Java\"],\"skillCount\":1}}。"
                        + "标签 5-15 个，权重 0-1。");
        req.setUserPrompt("请解析以下 JD：\n\n" + jdText);
        String raw = llmClient.chat(req);
        if (StringUtils.hasText(raw)) {
            try {
                String json = extractJson(raw);
                JdParseResultVO parsed = objectMapper.readValue(json, JdParseResultVO.class);
                if (parsed.getTags() != null && !parsed.getTags().isEmpty()) {
                    parsed.setRawText(jdText);
                    if (parsed.getEntities() == null) {
                        parsed.setEntities(new HashMap<>());
                    }
                    return parsed;
                }
            } catch (Exception e) {
                log.warn("LLM JD parse failed, fallback to rules: {}", e.getMessage());
            }
        }
        return simulateJdParsing(jdText);
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text.trim();
    }

    /**
     * Simulate LLM-based JD parsing.
     * Extracts keywords from predefined tech stack, assigns tri-weights based on frequency and position.
     */
    private JdParseResultVO simulateJdParsing(String jdText) {
        String jdLower = jdText.toLowerCase();

        List<TagDTO> tags = new ArrayList<>();
        Map<String, Object> entities = new HashMap<>();
        List<String> matchedSkills = new ArrayList<>();

        for (String keyword : TECH_KEYWORDS) {
            String kwLower = keyword.toLowerCase();
            int firstIndex = jdLower.indexOf(kwLower);
            if (firstIndex >= 0) {
                // Count occurrences
                int count = 0;
                int idx = 0;
                while ((idx = jdLower.indexOf(kwLower, idx)) >= 0) {
                    count++;
                    idx += kwLower.length();
                }

                // Calculate position bonus: keywords appearing earlier get higher weight
                double positionBonus = 1.0 - ((double) firstIndex / jdText.length()) * 0.3;

                // Frequency factor: more mentions = higher weight, capped at 1.0
                double frequencyFactor = Math.min(1.0, 0.5 + count * 0.15);

                // Base weight combines frequency and position
                double baseWeight = Math.min(1.0, frequencyFactor * positionBonus);

                TagDTO tag = new TagDTO();
                tag.setTag(keyword);
                tag.setMatchWeight(roundToTwo(Math.min(1.0, baseWeight * 1.0)));
                tag.setSearchWeight(roundToTwo(Math.min(1.0, baseWeight * 0.9)));
                tag.setDecisionWeight(roundToTwo(Math.min(1.0, baseWeight * 0.7)));
                tag.setLocked(false);

                tags.add(tag);
                matchedSkills.add(keyword);
            }
        }

        // Sort tags by match weight descending
        Collections.sort(tags, new Comparator<TagDTO>() {
            @Override
            public int compare(TagDTO a, TagDTO b) {
                return Double.compare(b.getMatchWeight(), a.getMatchWeight());
            }
        });

        // Build entities map
        entities.put("skills", matchedSkills);
        entities.put("skillCount", matchedSkills.size());
        entities.put("totalKeywords", TECH_KEYWORDS.size());

        JdParseResultVO result = new JdParseResultVO();
        result.setTags(tags);
        result.setEntities(entities);
        result.setRawText(jdText);

        return result;
    }

    /**
     * Round a double to 2 decimal places
     */
    private double roundToTwo(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * Generate job number: JOB + yyyyMMdd + 4-digit sequence
     */
    private String generateJobNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = jobSeqCounter.incrementAndGet();
        String seqStr = String.format("%04d", seq);
        return "JOB" + dateStr + seqStr;
    }

    /**
     * Convert entity to VO
     */
    private JobVO convertToVO(JobPosition job) {
        JobVO vo = new JobVO();
        vo.setId(job.getId());
        vo.setTenantId(job.getTenantId());
        vo.setDemandId(job.getDemandId());
        vo.setJobNo(job.getJobNo());
        vo.setTitle(job.getTitle());
        vo.setJdText(job.getJdText());
        vo.setJdParsedJson(job.getJdParsedJson());
        vo.setTags(job.getTags());
        vo.setEmbeddingVectorId(job.getEmbeddingVectorId());
        vo.setHeadCount(job.getHeadCount());
        vo.setFilledCount(job.getFilledCount());
        vo.setStatus(job.getStatus());
        vo.setClosedReason(job.getClosedReason());
        vo.setCreatedAt(job.getCreatedAt());
        vo.setUpdatedAt(job.getUpdatedAt());

        // Note: demandTitle and demandNo are not populated here.
        // In a full implementation, these would be loaded from the demand module via Feign or join query.
        // For now, they remain null unless populated externally.

        return vo;
    }
}
