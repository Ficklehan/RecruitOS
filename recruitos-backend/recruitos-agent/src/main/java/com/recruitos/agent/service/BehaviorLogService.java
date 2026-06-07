package com.recruitos.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruitos.agent.dto.BehaviorLogQueryDTO;
import com.recruitos.agent.dto.BehaviorLogVO;
import com.recruitos.agent.entity.AgentBehaviorLog;
import com.recruitos.agent.mapper.AgentBehaviorLogMapper;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.tenant.TenantContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * Behavior log service - business logic for agent behavior log management
 */
@Service
public class BehaviorLogService {

    @Resource
    private AgentBehaviorLogMapper behaviorLogMapper;

    /**
     * Get paginated behavior log list with filters
     */
    public PageResult<BehaviorLogVO> getLogList(BehaviorLogQueryDTO query) {
        Long tenantId = TenantContext.getTenantId();

        Page<AgentBehaviorLog> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<AgentBehaviorLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentBehaviorLog::getTenantId, tenantId);

        if (query.getAgentTaskId() != null) {
            wrapper.eq(AgentBehaviorLog::getAgentTaskId, query.getAgentTaskId());
        }
        if (query.getAgentAccountId() != null) {
            wrapper.eq(AgentBehaviorLog::getAgentAccountId, query.getAgentAccountId());
        }
        if (query.getActionType() != null && !query.getActionType().isEmpty()) {
            wrapper.eq(AgentBehaviorLog::getActionType, query.getActionType());
        }
        if (query.getIsSuccess() != null) {
            wrapper.eq(AgentBehaviorLog::getIsSuccess, query.getIsSuccess());
        }

        wrapper.orderByDesc(AgentBehaviorLog::getExecutedAt);

        Page<AgentBehaviorLog> result = behaviorLogMapper.selectPage(page, wrapper);

        List<BehaviorLogVO> voList = new ArrayList<>();
        for (AgentBehaviorLog log : result.getRecords()) {
            voList.add(convertToVO(log));
        }

        return new PageResult<>(result.getTotal(), voList, query.getPageNum(), query.getPageSize());
    }

    /**
     * Get statistics by action type and success rate
     */
    public Map<String, Object> getLogStats() {
        Long tenantId = TenantContext.getTenantId();

        LambdaQueryWrapper<AgentBehaviorLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentBehaviorLog::getTenantId, tenantId);
        List<AgentBehaviorLog> allLogs = behaviorLogMapper.selectList(wrapper);

        // Stats by action type
        Map<String, Map<String, Object>> byActionType = new LinkedHashMap<>();
        // Stats by success
        long totalSuccess = 0;
        long totalFailed = 0;

        for (AgentBehaviorLog log : allLogs) {
            String actionType = log.getActionType();
            if (actionType == null) {
                actionType = "OTHER";
            }

            Map<String, Object> typeStats = byActionType.get(actionType);
            if (typeStats == null) {
                typeStats = new LinkedHashMap<>();
                typeStats.put("actionType", actionType);
                typeStats.put("totalCount", 0L);
                typeStats.put("successCount", 0L);
                typeStats.put("failedCount", 0L);
                typeStats.put("successRate", 0.0);
                byActionType.put(actionType, typeStats);
            }

            long totalCount = (long) typeStats.get("totalCount") + 1;
            typeStats.put("totalCount", totalCount);

            if (log.getIsSuccess() != null && log.getIsSuccess() == 1) {
                long successCount = (long) typeStats.get("successCount") + 1;
                typeStats.put("successCount", successCount);
                totalSuccess++;
            } else {
                long failedCount = (long) typeStats.get("failedCount") + 1;
                typeStats.put("failedCount", failedCount);
                totalFailed++;
            }

            double successRate = (double) (long) typeStats.get("successCount") / totalCount * 100;
            typeStats.put("successRate", Math.round(successRate * 10.0) / 10.0);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalLogs", allLogs.size());
        stats.put("totalSuccess", totalSuccess);
        stats.put("totalFailed", totalFailed);
        double overallSuccessRate = allLogs.isEmpty() ? 0.0 : (double) totalSuccess / allLogs.size() * 100;
        stats.put("overallSuccessRate", Math.round(overallSuccessRate * 10.0) / 10.0);
        stats.put("byActionType", new ArrayList<>(byActionType.values()));

        return stats;
    }

    /**
     * Get activity timeline for a specific account
     */
    public PageResult<BehaviorLogVO> getAccountActivity(Long accountId) {
        Long tenantId = TenantContext.getTenantId();

        LambdaQueryWrapper<AgentBehaviorLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentBehaviorLog::getTenantId, tenantId)
                .eq(AgentBehaviorLog::getAgentAccountId, accountId)
                .orderByDesc(AgentBehaviorLog::getExecutedAt);

        List<AgentBehaviorLog> logs = behaviorLogMapper.selectList(wrapper);

        List<BehaviorLogVO> voList = new ArrayList<>();
        for (AgentBehaviorLog log : logs) {
            voList.add(convertToVO(log));
        }

        return new PageResult<>(voList.size(), voList, 1, voList.size());
    }

    /**
     * Convert AgentBehaviorLog entity to BehaviorLogVO
     */
    private BehaviorLogVO convertToVO(AgentBehaviorLog log) {
        BehaviorLogVO vo = new BehaviorLogVO();
        vo.setId(log.getId());
        vo.setTenantId(log.getTenantId());
        vo.setAgentTaskId(log.getAgentTaskId());
        vo.setAgentAccountId(log.getAgentAccountId());
        vo.setActionType(log.getActionType());
        vo.setTargetName(log.getTargetName());
        vo.setTargetPlatform(log.getTargetPlatform());
        vo.setActionDetail(log.getActionDetail());
        vo.setIsSuccess(log.getIsSuccess());
        vo.setErrorMessage(log.getErrorMessage());
        vo.setRandomDelay(log.getRandomDelay());
        vo.setExecutedAt(log.getExecutedAt());
        vo.setCreatedAt(log.getCreatedAt());
        return vo;
    }
}
