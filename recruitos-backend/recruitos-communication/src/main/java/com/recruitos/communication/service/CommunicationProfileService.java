package com.recruitos.communication.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitos.common.auth.CurrentUser;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.communication.dto.CommunicationProfileSaveDTO;
import com.recruitos.communication.dto.CommunicationProfileVO;
import com.recruitos.communication.entity.CommunicationProfile;
import com.recruitos.communication.mapper.CommunicationProfileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class CommunicationProfileService {

    private static final List<String> DEFAULT_TRIGGERS = Arrays.asList("HAS_CHAT_REPLY", "SILENCE_48H");

    @Resource
    private CommunicationProfileMapper profileMapper;
    @Resource
    private ObjectMapper objectMapper;

    public CommunicationProfileVO getTenantDefault() {
        return toVO(findProfile(null), false);
    }

    @Transactional
    public CommunicationProfileVO saveTenantDefault(CommunicationProfileSaveDTO dto) {
        return saveProfile(null, dto);
    }

    public CommunicationProfileVO getJobOverride(Long jobId) {
        CommunicationProfile job = findProfile(jobId);
        if (job == null) {
            CommunicationProfileVO vo = new CommunicationProfileVO();
            vo.setJobId(jobId);
            vo.setJobOverride(false);
            return vo;
        }
        CommunicationProfileVO vo = toVO(job, true);
        vo.setJobId(jobId);
        return vo;
    }

    @Transactional
    public CommunicationProfileVO saveJobOverride(Long jobId, CommunicationProfileSaveDTO dto) {
        return saveProfile(jobId, dto);
    }

    /** 租户默认 merge 岗位覆盖，供 Campaign / 聊天读取 */
    public CommunicationProfileVO resolveForJob(Long jobId) {
        CommunicationProfile tenant = findProfile(null);
        CommunicationProfile job = jobId != null ? findProfile(jobId) : null;
        CommunicationProfileVO vo = toVO(tenant, false);
        vo.setJobId(jobId);
        if (job != null) {
            mergeOverride(vo, job);
            vo.setJobOverride(true);
        }
        return vo;
    }

    private CommunicationProfile findProfile(Long jobId) {
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<CommunicationProfile> w = new LambdaQueryWrapper<>();
        w.eq(CommunicationProfile::getTenantId, tenantId);
        if (jobId == null) {
            w.isNull(CommunicationProfile::getJobId);
        } else {
            w.eq(CommunicationProfile::getJobId, jobId);
        }
        w.last("LIMIT 1");
        return profileMapper.selectOne(w);
    }

    private CommunicationProfileVO saveProfile(Long jobId, CommunicationProfileSaveDTO dto) {
        scanGuardrails(dto.getGuardrails());
        Long tenantId = TenantContext.getTenantId();
        CommunicationProfile existing = findProfile(jobId);
        CommunicationProfile entity = existing != null ? existing : new CommunicationProfile();
        entity.setTenantId(tenantId);
        entity.setJobId(jobId);
        entity.setPersona(dto.getPersona());
        entity.setCompanyBackground(dto.getCompanyBackground());
        entity.setCommunicationLogic(dto.getCommunicationLogic());
        entity.setGuardrails(dto.getGuardrails());
        entity.setCreatedBy(CurrentUser.getCurrentUserId());
        try {
            entity.setProactiveTriggersJson(objectMapper.writeValueAsString(
                    dto.getProactiveTriggers() != null ? dto.getProactiveTriggers() : DEFAULT_TRIGGERS));
        } catch (Exception e) {
            throw new BizException("触发器序列化失败");
        }
        if (existing == null) {
            profileMapper.insert(entity);
        } else {
            profileMapper.updateById(entity);
        }
        CommunicationProfileVO vo = toVO(entity, jobId != null);
        vo.setJobId(jobId);
        return vo;
    }

    private void mergeOverride(CommunicationProfileVO base, CommunicationProfile job) {
        if (StringUtils.hasText(job.getPersona())) {
            base.setPersona(job.getPersona());
        }
        if (StringUtils.hasText(job.getCompanyBackground())) {
            base.setCompanyBackground(job.getCompanyBackground());
        }
        if (StringUtils.hasText(job.getCommunicationLogic())) {
            base.setCommunicationLogic(job.getCommunicationLogic());
        }
        if (StringUtils.hasText(job.getGuardrails())) {
            base.setGuardrails(job.getGuardrails());
        }
        List<String> triggers = parseTriggers(job.getProactiveTriggersJson());
        if (triggers != null && !triggers.isEmpty()) {
            base.setProactiveTriggers(triggers);
        }
    }

    private void scanGuardrails(String guardrails) {
        if (!StringUtils.hasText(guardrails)) {
            return;
        }
        String lower = guardrails.toLowerCase();
        if (lower.contains("歧视") || lower.contains("限男") || lower.contains("限女")) {
            throw new BizException("沟通护栏包含不合规内容");
        }
    }

    private CommunicationProfileVO toVO(CommunicationProfile entity, boolean jobOverride) {
        CommunicationProfileVO vo = new CommunicationProfileVO();
        if (entity == null) {
            vo.setPersona("专业、简洁、尊重候选人的招聘顾问");
            vo.setCompanyBackground("请补充公司背景");
            vo.setCommunicationLogic("先确认意向，再介绍岗位亮点，最后索要简历");
            vo.setProactiveTriggers(new ArrayList<>(DEFAULT_TRIGGERS));
            vo.setGuardrails("不承诺薪资；不歧视；不过度施压");
            vo.setJobOverride(jobOverride);
            return vo;
        }
        vo.setId(entity.getId());
        vo.setJobId(entity.getJobId());
        vo.setPersona(entity.getPersona());
        vo.setCompanyBackground(entity.getCompanyBackground());
        vo.setCommunicationLogic(entity.getCommunicationLogic());
        vo.setGuardrails(entity.getGuardrails());
        vo.setProactiveTriggers(parseTriggers(entity.getProactiveTriggersJson()));
        vo.setJobOverride(jobOverride);
        return vo;
    }

    private List<String> parseTriggers(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>(DEFAULT_TRIGGERS);
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>(DEFAULT_TRIGGERS);
        }
    }
}
