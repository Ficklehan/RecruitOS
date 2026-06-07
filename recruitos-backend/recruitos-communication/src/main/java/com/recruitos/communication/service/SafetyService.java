package com.recruitos.communication.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.communication.dto.SafetyLogQueryDTO;
import com.recruitos.communication.dto.SafetyLogVO;
import com.recruitos.communication.entity.SafetyLog;
import com.recruitos.communication.mapper.SafetyLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Safety service - business logic for message safety check management
 */
@Service
public class SafetyService {

    @Resource
    private SafetyLogMapper safetyLogMapper;

    /**
     * Get paginated safety log list with filters
     */
    public PageResult<SafetyLogVO> getSafetyLogList(SafetyLogQueryDTO query) {
        Long tenantId = TenantContext.getTenantId();

        Page<SafetyLog> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<SafetyLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SafetyLog::getTenantId, tenantId);

        if (query.getConversationId() != null) {
            wrapper.eq(SafetyLog::getConversationId, query.getConversationId());
        }
        if (StringUtils.hasText(query.getCheckType())) {
            wrapper.eq(SafetyLog::getCheckType, query.getCheckType());
        }
        if (StringUtils.hasText(query.getCheckResult())) {
            wrapper.eq(SafetyLog::getCheckResult, query.getCheckResult());
        }
        if (StringUtils.hasText(query.getRiskLevel())) {
            wrapper.eq(SafetyLog::getRiskLevel, query.getRiskLevel());
        }
        if (StringUtils.hasText(query.getAction())) {
            wrapper.eq(SafetyLog::getAction, query.getAction());
        }

        wrapper.orderByDesc(SafetyLog::getCheckedAt);

        Page<SafetyLog> result = safetyLogMapper.selectPage(page, wrapper);

        List<SafetyLogVO> voList = new ArrayList<>();
        for (SafetyLog log : result.getRecords()) {
            voList.add(convertToVO(log));
        }

        return new PageResult<>(result.getTotal(), voList, query.getPageNum(), query.getPageSize());
    }

    /**
     * Get safety statistics grouped by check type and result
     */
    public Map<String, Object> getSafetyStats() {
        Long tenantId = TenantContext.getTenantId();

        LambdaQueryWrapper<SafetyLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SafetyLog::getTenantId, tenantId);
        List<SafetyLog> logs = safetyLogMapper.selectList(wrapper);

        int totalChecks = logs.size();
        Map<String, Integer> byCheckType = new HashMap<>();
        Map<String, Integer> byCheckResult = new HashMap<>();
        Map<String, Integer> byRiskLevel = new HashMap<>();

        for (SafetyLog log : logs) {
            // Count by check type
            String checkType = log.getCheckType();
            if (checkType != null) {
                byCheckType.put(checkType, byCheckType.getOrDefault(checkType, 0) + 1);
            }

            // Count by check result
            String checkResult = log.getCheckResult();
            if (checkResult != null) {
                byCheckResult.put(checkResult, byCheckResult.getOrDefault(checkResult, 0) + 1);
            }

            // Count by risk level
            String riskLevel = log.getRiskLevel();
            if (riskLevel != null) {
                byRiskLevel.put(riskLevel, byRiskLevel.getOrDefault(riskLevel, 0) + 1);
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalChecks", totalChecks);
        stats.put("byCheckType", byCheckType);
        stats.put("byCheckResult", byCheckResult);
        stats.put("byRiskLevel", byRiskLevel);

        return stats;
    }

    /**
     * Manual review of a safety log entry
     */
    @Transactional
    public SafetyLogVO reviewMessage(Long logId, String action) {
        Long tenantId = TenantContext.getTenantId();

        SafetyLog log = safetyLogMapper.selectById(logId);
        if (log == null) {
            throw new BizException("Safety log not found");
        }
        if (!log.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        log.setAction(action);
        safetyLogMapper.updateById(log);

        return convertToVO(log);
    }

    /**
     * Convert SafetyLog entity to SafetyLogVO
     */
    private SafetyLogVO convertToVO(SafetyLog log) {
        SafetyLogVO vo = new SafetyLogVO();
        vo.setId(log.getId());
        vo.setConversationId(log.getConversationId());
        vo.setMessageId(log.getMessageId());
        vo.setCheckType(log.getCheckType());
        vo.setCheckResult(log.getCheckResult());
        vo.setMatchedContent(log.getMatchedContent());
        vo.setRiskLevel(log.getRiskLevel());
        vo.setAction(log.getAction());
        vo.setCheckedAt(log.getCheckedAt());
        vo.setCreatedAt(log.getCreatedAt());
        return vo;
    }
}
