package com.recruitos.communication.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.communication.dto.TemplateCreateDTO;
import com.recruitos.communication.dto.TemplateQueryDTO;
import com.recruitos.communication.dto.TemplateVO;
import com.recruitos.communication.entity.MessageTemplate;
import com.recruitos.communication.mapper.MessageTemplateMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TemplateService {

    @Resource
    private MessageTemplateMapper templateMapper;

    @Transactional
    public TemplateVO createTemplate(TemplateCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BizException("User not authenticated");
        }

        MessageTemplate template = new MessageTemplate();
        template.setTenantId(tenantId);
        template.setTitle(dto.getTemplateName());
        template.setScene(StringUtils.hasText(dto.getTemplateType()) ? dto.getTemplateType() : "INITIAL");
        template.setCandidateType("ALL");
        template.setContent(dto.getContent());
        template.setStatus(toDbStatus(dto.getStatus()));
        template.setAbGroup(dto.getAbTestGroup());
        template.setIsAbTest(StringUtils.hasText(dto.getAbTestGroup()) ? 1 : 0);
        template.setSendCount(0);
        template.setReplyCount(0);
        template.setResumeCount(0);
        template.setHireCount(0);
        template.setPriority(0);

        templateMapper.insert(template);
        return convertToVO(template);
    }

    @Transactional
    public TemplateVO updateTemplate(Long id, TemplateCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();

        MessageTemplate template = requireTemplate(id, tenantId);

        if (StringUtils.hasText(dto.getTemplateName())) {
            template.setTitle(dto.getTemplateName());
        }
        if (StringUtils.hasText(dto.getTemplateType())) {
            template.setScene(dto.getTemplateType());
        }
        if (StringUtils.hasText(dto.getContent())) {
            template.setContent(dto.getContent());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            template.setStatus(toDbStatus(dto.getStatus()));
        }
        if (dto.getAbTestGroup() != null) {
            template.setAbGroup(dto.getAbTestGroup());
            template.setIsAbTest(StringUtils.hasText(dto.getAbTestGroup()) ? 1 : 0);
        }

        templateMapper.updateById(template);
        return convertToVO(template);
    }

    public PageResult<TemplateVO> getTemplateList(TemplateQueryDTO query) {
        Long tenantId = TenantContext.getTenantId();
        int pageNum = query.getPageNum() != null && query.getPageNum() > 0 ? query.getPageNum() : 1;
        int pageSize = query.getPageSize() != null && query.getPageSize() > 0 ? query.getPageSize() : 10;

        Page<MessageTemplate> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MessageTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageTemplate::getTenantId, tenantId);

        if (StringUtils.hasText(query.getTemplateType())) {
            wrapper.eq(MessageTemplate::getScene, query.getTemplateType());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(MessageTemplate::getStatus, toDbStatus(query.getStatus()));
        }
        if (StringUtils.hasText(query.getTemplateName())) {
            wrapper.like(MessageTemplate::getTitle, query.getTemplateName());
        }

        wrapper.orderByDesc(MessageTemplate::getCreatedAt);
        Page<MessageTemplate> result = templateMapper.selectPage(page, wrapper);

        List<TemplateVO> voList = new ArrayList<>();
        for (MessageTemplate template : result.getRecords()) {
            voList.add(convertToVO(template));
        }
        return new PageResult<>(result.getTotal(), voList, pageNum, pageSize);
    }

    public TemplateVO getTemplateDetail(Long id) {
        Long tenantId = TenantContext.getTenantId();
        return convertToVO(requireTemplate(id, tenantId));
    }

    @Transactional
    public void deleteTemplate(Long id) {
        Long tenantId = TenantContext.getTenantId();
        MessageTemplate template = requireTemplate(id, tenantId);
        template.setStatus(0);
        templateMapper.updateById(template);
    }

    public Map<String, Object> getTemplateStats() {
        Long tenantId = TenantContext.getTenantId();

        LambdaQueryWrapper<MessageTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageTemplate::getTenantId, tenantId);
        List<MessageTemplate> templates = templateMapper.selectList(wrapper);

        int totalTemplates = templates.size();
        int activeTemplates = 0;
        int totalUsage = 0;
        BigDecimal totalSuccessRate = BigDecimal.ZERO;
        Map<String, Integer> typeDistribution = new HashMap<>();

        for (MessageTemplate template : templates) {
            if (isActive(template.getStatus())) {
                activeTemplates++;
            }
            int send = template.getSendCount() != null ? template.getSendCount() : 0;
            totalUsage += send;
            totalSuccessRate = totalSuccessRate.add(computeSuccessRate(template));
            String type = template.getScene();
            if (type != null) {
                typeDistribution.put(type, typeDistribution.getOrDefault(type, 0) + 1);
            }
        }

        BigDecimal avgSuccessRate = totalTemplates > 0
                ? totalSuccessRate.divide(BigDecimal.valueOf(totalTemplates), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTemplates", totalTemplates);
        stats.put("activeTemplates", activeTemplates);
        stats.put("totalUsage", totalUsage);
        stats.put("avgSuccessRate", avgSuccessRate);
        stats.put("typeDistribution", typeDistribution);
        return stats;
    }

    private MessageTemplate requireTemplate(Long id, Long tenantId) {
        MessageTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BizException("Template not found");
        }
        if (!template.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        return template;
    }

    private TemplateVO convertToVO(MessageTemplate template) {
        TemplateVO vo = new TemplateVO();
        vo.setId(template.getId());
        vo.setTemplateName(template.getTitle());
        vo.setTemplateType(template.getScene());
        vo.setContent(template.getContent());
        vo.setStatus(fromDbStatus(template.getStatus()));
        vo.setAbTestGroup(template.getAbGroup());
        vo.setUsageCount(template.getSendCount() != null ? template.getSendCount() : 0);
        vo.setSuccessRate(computeSuccessRate(template));
        vo.setCreatedAt(template.getCreatedAt());
        vo.setUpdatedAt(template.getUpdatedAt());
        return vo;
    }

    private BigDecimal computeSuccessRate(MessageTemplate template) {
        int send = template.getSendCount() != null ? template.getSendCount() : 0;
        int reply = template.getReplyCount() != null ? template.getReplyCount() : 0;
        if (send <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(reply)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(send), 2, RoundingMode.HALF_UP);
    }

    private int toDbStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return 1;
        }
        String s = status.trim().toUpperCase();
        if ("0".equals(s) || "DISABLED".equals(s) || "INACTIVE".equals(s) || "OFF".equals(s)) {
            return 0;
        }
        return 1;
    }

    private String fromDbStatus(Integer status) {
        return isActive(status) ? "ACTIVE" : "INACTIVE";
    }

    private boolean isActive(Integer status) {
        return status == null || status != 0;
    }
}
