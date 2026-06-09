package com.recruitos.job.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitos.common.auth.CurrentUser;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.job.dto.SourcingOpsPackVO;
import com.recruitos.job.dto.TagDTO;
import com.recruitos.job.entity.JobPosition;
import com.recruitos.job.entity.JobSourcingOpsPack;
import com.recruitos.job.mapper.JobPositionMapper;
import com.recruitos.job.mapper.JobSourcingOpsPackMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SourcingOpsPackService {

    private static final Pattern DISCRIMINATION_PATTERN = Pattern.compile(
            "35周岁|30周岁|限男|限女|男性优先|女性优先|已婚|未婚|统招全日制.*985|本地户口|户籍",
            Pattern.CASE_INSENSITIVE);

    @Resource
    private JobSourcingOpsPackMapper opsPackMapper;
    @Resource
    private JobPositionMapper jobPositionMapper;
    @Resource
    private JobService jobService;
    @Resource
    private ObjectMapper objectMapper;

    @Transactional
    public SourcingOpsPackVO generateDraft(Long jobId) {
        JobPosition job = requireJob(jobId);
        List<TagDTO> tags = jobService.getTags(jobId);
        if (tags == null || tags.isEmpty()) {
            throw new BizException("请先解析 JD 并生成标签后再生成运营包");
        }

        int nextVersion = nextVersion(jobId);
        Map<String, Object> pack = buildPackFromTags(job, tags, nextVersion);
        scanCompliance(pack);

        JobSourcingOpsPack entity = new JobSourcingOpsPack();
        entity.setJobId(jobId);
        entity.setVersion(nextVersion);
        entity.setStatus("DRAFT");
        entity.setCreatedBy(CurrentUser.getCurrentUserId());
        try {
            entity.setPackJson(objectMapper.writeValueAsString(pack));
        } catch (Exception e) {
            throw new BizException("运营包序列化失败");
        }
        opsPackMapper.insert(entity);
        return toVO(entity);
    }

    public SourcingOpsPackVO getActive(Long jobId) {
        JobSourcingOpsPack active = findByStatus(jobId, "ACTIVE");
        return active != null ? toVO(active) : null;
    }

    public List<SourcingOpsPackVO> listVersions(Long jobId) {
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<JobSourcingOpsPack> w = new LambdaQueryWrapper<>();
        w.eq(JobSourcingOpsPack::getTenantId, tenantId)
                .eq(JobSourcingOpsPack::getJobId, jobId)
                .orderByDesc(JobSourcingOpsPack::getVersion);
        List<SourcingOpsPackVO> list = new ArrayList<>();
        for (JobSourcingOpsPack p : opsPackMapper.selectList(w)) {
            list.add(toVO(p));
        }
        return list;
    }

    public SourcingOpsPackVO getById(Long jobId, Long packId) {
        JobSourcingOpsPack pack = requirePack(jobId, packId);
        return toVO(pack);
    }

    @Transactional
    public SourcingOpsPackVO updateDraft(Long jobId, Long packId, Map<String, Object> packBody) {
        JobSourcingOpsPack pack = requirePack(jobId, packId);
        if (!"DRAFT".equals(pack.getStatus())) {
            throw new BizException("仅 DRAFT 状态运营包可编辑");
        }
        scanCompliance(packBody);
        try {
            pack.setPackJson(objectMapper.writeValueAsString(packBody));
        } catch (Exception e) {
            throw new BizException("运营包序列化失败");
        }
        opsPackMapper.updateById(pack);
        return toVO(pack);
    }

    @Transactional
    public SourcingOpsPackVO confirm(Long jobId, Long packId) {
        JobSourcingOpsPack pack = requirePack(jobId, packId);
        if (!"DRAFT".equals(pack.getStatus())) {
            throw new BizException("仅 DRAFT 状态运营包可确认");
        }

        Long tenantId = TenantContext.getTenantId();
        LambdaUpdateWrapper<JobSourcingOpsPack> archive = new LambdaUpdateWrapper<>();
        archive.eq(JobSourcingOpsPack::getTenantId, tenantId)
                .eq(JobSourcingOpsPack::getJobId, jobId)
                .eq(JobSourcingOpsPack::getStatus, "ACTIVE")
                .set(JobSourcingOpsPack::getStatus, "ARCHIVED");
        opsPackMapper.update(null, archive);

        pack.setStatus("ACTIVE");
        pack.setConfirmedBy(CurrentUser.getCurrentUserId());
        pack.setConfirmedAt(LocalDateTime.now());
        opsPackMapper.updateById(pack);
        return toVO(pack);
    }

    @Transactional
    public SourcingOpsPackVO applyFromEvolution(Long jobId, Map<String, Object> packBody, Long proposalId) {
        requireJob(jobId);
        scanCompliance(packBody);

        Long tenantId = TenantContext.getTenantId();
        LambdaUpdateWrapper<JobSourcingOpsPack> archive = new LambdaUpdateWrapper<>();
        archive.eq(JobSourcingOpsPack::getTenantId, tenantId)
                .eq(JobSourcingOpsPack::getJobId, jobId)
                .eq(JobSourcingOpsPack::getStatus, "ACTIVE")
                .set(JobSourcingOpsPack::getStatus, "ARCHIVED");
        opsPackMapper.update(null, archive);

        int nextVersion = nextVersion(jobId);
        packBody.put("version", nextVersion);

        JobSourcingOpsPack entity = new JobSourcingOpsPack();
        entity.setJobId(jobId);
        entity.setVersion(nextVersion);
        entity.setStatus("ACTIVE");
        entity.setProposalId(proposalId);
        entity.setConfirmedBy(CurrentUser.getCurrentUserId());
        entity.setConfirmedAt(LocalDateTime.now());
        entity.setCreatedBy(CurrentUser.getCurrentUserId());
        try {
            entity.setPackJson(objectMapper.writeValueAsString(packBody));
        } catch (Exception e) {
            throw new BizException("运营包序列化失败");
        }
        opsPackMapper.insert(entity);
        return toVO(entity);
    }

    private Map<String, Object> buildPackFromTags(JobPosition job, List<TagDTO> tags, int version) {
        Map<String, Object> pack = new LinkedHashMap<>();
        pack.put("version", version);
        pack.put("jobId", job.getId());
        pack.put("jobTitle", job.getTitle());

        List<Map<String, Object>> stage1 = new ArrayList<>();
        List<Map<String, Object>> stage2 = new ArrayList<>();
        for (TagDTO tag : tags) {
            double mw = tag.getMatchWeight() != null ? tag.getMatchWeight() : 0.5;
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("tag", tag.getTag());
            rule.put("minScore", mw >= 0.8 ? 1.0 : 0.5);
            rule.put("required", mw >= 0.8);
            if (mw >= 0.8) {
                stage2.add(rule);
            } else {
                stage1.add(rule);
            }
        }

        Map<String, Object> screening = new LinkedHashMap<>();
        screening.put("stage1CardRules", stage1);
        screening.put("stage2ResumeRules", stage2);
        screening.put("passThreshold", 60);
        pack.put("screeningProfile", screening);

        List<String> keywords = tags.stream()
                .sorted((a, b) -> Double.compare(
                        b.getSearchWeight() != null ? b.getSearchWeight() : 0,
                        a.getSearchWeight() != null ? a.getSearchWeight() : 0))
                .limit(8)
                .map(TagDTO::getTag)
                .collect(Collectors.toList());
        pack.put("searchKeywords", keywords);

        Map<String, Object> comm = new LinkedHashMap<>();
        comm.put("persona", "专业、简洁、尊重候选人的招聘顾问");
        comm.put("companyBackground", "请补充公司背景介绍");
        comm.put("jobInfoFromJd", true);
        comm.put("communicationLogic", "先确认意向，再介绍岗位亮点，最后索要简历");
        comm.put("proactiveTriggers", Collections.singletonList("HAS_CHAT_REPLY"));
        comm.put("guardrails", "不承诺薪资；不歧视；不过度施压");
        pack.put("communicationProfile", comm);

        Map<String, Object> rechat = new LinkedHashMap<>();
        rechat.put("maxAttempts", 2);
        rechat.put("intervalHours", 48);
        pack.put("rechatPolicy", rechat);

        pack.put("greetStrategy", "SCREEN_THEN_GREET");
        Map<String, Object> quotas = new LinkedHashMap<>();
        quotas.put("BOSS", 30);
        quotas.put("LIEPIN", 30);
        pack.put("platformQuotas", quotas);

        return pack;
    }

    private void scanCompliance(Map<String, Object> pack) {
        String json;
        try {
            json = objectMapper.writeValueAsString(pack);
        } catch (Exception e) {
            throw new BizException("合规扫描失败");
        }
        if (DISCRIMINATION_PATTERN.matcher(json).find()) {
            throw new BizException("运营包包含歧视性或不合规筛选条件，请修改后保存");
        }
    }

    private int nextVersion(Long jobId) {
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<JobSourcingOpsPack> w = new LambdaQueryWrapper<>();
        w.eq(JobSourcingOpsPack::getTenantId, tenantId)
                .eq(JobSourcingOpsPack::getJobId, jobId)
                .orderByDesc(JobSourcingOpsPack::getVersion)
                .last("LIMIT 1");
        JobSourcingOpsPack latest = opsPackMapper.selectOne(w);
        return latest == null ? 1 : latest.getVersion() + 1;
    }

    private JobSourcingOpsPack findByStatus(Long jobId, String status) {
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<JobSourcingOpsPack> w = new LambdaQueryWrapper<>();
        w.eq(JobSourcingOpsPack::getTenantId, tenantId)
                .eq(JobSourcingOpsPack::getJobId, jobId)
                .eq(JobSourcingOpsPack::getStatus, status)
                .orderByDesc(JobSourcingOpsPack::getVersion)
                .last("LIMIT 1");
        return opsPackMapper.selectOne(w);
    }

    private JobPosition requireJob(Long jobId) {
        Long tenantId = TenantContext.getTenantId();
        JobPosition job = jobPositionMapper.selectById(jobId);
        if (job == null || !job.getTenantId().equals(tenantId)) {
            throw new BizException("岗位不存在");
        }
        return job;
    }

    private JobSourcingOpsPack requirePack(Long jobId, Long packId) {
        Long tenantId = TenantContext.getTenantId();
        JobSourcingOpsPack pack = opsPackMapper.selectById(packId);
        if (pack == null || !pack.getTenantId().equals(tenantId) || !pack.getJobId().equals(jobId)) {
            throw new BizException("运营包不存在");
        }
        return pack;
    }

    private SourcingOpsPackVO toVO(JobSourcingOpsPack entity) {
        SourcingOpsPackVO vo = new SourcingOpsPackVO();
        vo.setId(entity.getId());
        vo.setJobId(entity.getJobId());
        vo.setVersion(entity.getVersion());
        vo.setStatus(entity.getStatus());
        vo.setConfirmedBy(entity.getConfirmedBy());
        vo.setConfirmedAt(entity.getConfirmedAt());
        vo.setCreatedAt(entity.getCreatedAt());
        if (StringUtils.hasText(entity.getPackJson())) {
            try {
                vo.setPack(objectMapper.readValue(entity.getPackJson(), new TypeReference<Map<String, Object>>() {}));
            } catch (Exception e) {
                vo.setPack(entity.getPackJson());
            }
        }
        return vo;
    }
}
