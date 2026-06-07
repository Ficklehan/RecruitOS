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

/**
 * Template service - business logic for message template management
 */
@Service
public class TemplateService {

    @Resource
    private MessageTemplateMapper templateMapper;

    /**
     * Create a new message template
     */
    @Transactional
    public TemplateVO createTemplate(TemplateCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BizException("User not authenticated");
        }

        MessageTemplate template = new MessageTemplate();
        template.setTenantId(tenantId);
        template.setTemplateName(dto.getTemplateName());
        template.setTemplateType(dto.getTemplateType());
        template.setContent(dto.getContent());
        template.setVariables(dto.getVariables());
        template.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        template.setAbTestGroup(dto.getAbTestGroup());
        template.setUsageCount(0);
        template.setSuccessRate(BigDecimal.ZERO);
        template.setCreatedBy(tenantId);

        templateMapper.insert(template);

        return convertToVO(template);
    }

    /**
     * Update an existing message template
     */
    @Transactional
    public TemplateVO updateTemplate(Long id, TemplateCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();

        MessageTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BizException("Template not found");
        }
        if (!template.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        if (StringUtils.hasText(dto.getTemplateName())) {
            template.setTemplateName(dto.getTemplateName());
        }
        if (StringUtils.hasText(dto.getTemplateType())) {
            template.setTemplateType(dto.getTemplateType());
        }
        if (StringUtils.hasText(dto.getContent())) {
            template.setContent(dto.getContent());
        }
        if (dto.getVariables() != null) {
            template.setVariables(dto.getVariables());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            template.setStatus(dto.getStatus());
        }
        if (dto.getAbTestGroup() != null) {
            template.setAbTestGroup(dto.getAbTestGroup());
        }

        templateMapper.updateById(template);

        return convertToVO(template);
    }

    /**
     * Get paginated template list with filters
     */
    public PageResult<TemplateVO> getTemplateList(TemplateQueryDTO query) {
        Long tenantId = TenantContext.getTenantId();

        Page<MessageTemplate> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<MessageTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageTemplate::getTenantId, tenantId);

        if (StringUtils.hasText(query.getTemplateType())) {
            wrapper.eq(MessageTemplate::getTemplateType, query.getTemplateType());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(MessageTemplate::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getTemplateName())) {
            wrapper.like(MessageTemplate::getTemplateName, query.getTemplateName());
        }

        wrapper.orderByDesc(MessageTemplate::getCreatedAt);

        Page<MessageTemplate> result = templateMapper.selectPage(page, wrapper);

        List<TemplateVO> voList = new ArrayList<>();
        for (MessageTemplate template : result.getRecords()) {
            voList.add(convertToVO(template));
        }

        return new PageResult<>(result.getTotal(), voList, query.getPageNum(), query.getPageSize());
    }

    /**
     * Get template detail by ID
     */
    public TemplateVO getTemplateDetail(Long id) {
        Long tenantId = TenantContext.getTenantId();

        MessageTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BizException("Template not found");
        }
        if (!template.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        return convertToVO(template);
    }

    /**
     * Soft delete a template (set status to DISABLED)
     */
    @Transactional
    public void deleteTemplate(Long id) {
        Long tenantId = TenantContext.getTenantId();

        MessageTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BizException("Template not found");
        }
        if (!template.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        template.setStatus("DISABLED");
        templateMapper.updateById(template);
    }

    /**
     * Get template usage statistics
     */
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
            if ("ACTIVE".equals(template.getStatus())) {
                activeTemplates++;
            }
            if (template.getUsageCount() != null) {
                totalUsage += template.getUsageCount();
            }
            if (template.getSuccessRate() != null) {
                totalSuccessRate = totalSuccessRate.add(template.getSuccessRate());
            }
            String type = template.getTemplateType();
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

    /**
     * Convert MessageTemplate entity to TemplateVO
     */
    private TemplateVO convertToVO(MessageTemplate template) {
        TemplateVO vo = new TemplateVO();
        vo.setId(template.getId());
        vo.setTemplateName(template.getTemplateName());
        vo.setTemplateType(template.getTemplateType());
        vo.setContent(template.getContent());
        vo.setVariables(template.getVariables());
        vo.setStatus(template.getStatus());
        vo.setAbTestGroup(template.getAbTestGroup());
        vo.setUsageCount(template.getUsageCount());
        vo.setSuccessRate(template.getSuccessRate());
        vo.setCreatedBy(template.getCreatedBy());
        vo.setCreatedAt(template.getCreatedAt());
        vo.setUpdatedAt(template.getUpdatedAt());
        return vo;
    }
}
