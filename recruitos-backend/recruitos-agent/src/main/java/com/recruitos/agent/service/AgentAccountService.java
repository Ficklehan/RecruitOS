package com.recruitos.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruitos.agent.dto.AgentAccountCreateDTO;
import com.recruitos.agent.dto.AgentAccountQueryDTO;
import com.recruitos.agent.dto.AgentAccountVO;
import com.recruitos.agent.entity.AgentAccount;
import com.recruitos.agent.entity.AgentBehaviorLog;
import com.recruitos.agent.mapper.AgentAccountMapper;
import com.recruitos.agent.mapper.AgentBehaviorLogMapper;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent account service - business logic for agent account management
 */
@Service
public class AgentAccountService {

    @Resource
    private AgentAccountMapper accountMapper;

    @Resource
    private AgentBehaviorLogMapper behaviorLogMapper;

    /**
     * Create a new agent account
     */
    public AgentAccountVO createAccount(AgentAccountCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();

        AgentAccount account = new AgentAccount();
        account.setTenantId(tenantId);
        account.setPlatform(dto.getPlatform());
        account.setAccountName(dto.getAccountName());
        account.setAccountId(dto.getAccountId());
        account.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        account.setHealthScore(100.0);
        account.setDailyLimit(dto.getDailyLimit());
        account.setUsedToday(0);
        account.setRemark(dto.getRemark());
        account.setCreatedBy(null); // Will be set by auth context
        accountMapper.insert(account);

        return convertToVO(account);
    }

    /**
     * Update an existing agent account
     */
    public AgentAccountVO updateAccount(Long id, AgentAccountCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();

        AgentAccount account = accountMapper.selectById(id);
        if (account == null) {
            throw new BizException("Agent account not found");
        }
        if (!account.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        if (StringUtils.hasText(dto.getPlatform())) {
            account.setPlatform(dto.getPlatform());
        }
        if (StringUtils.hasText(dto.getAccountName())) {
            account.setAccountName(dto.getAccountName());
        }
        if (StringUtils.hasText(dto.getAccountId())) {
            account.setAccountId(dto.getAccountId());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            account.setStatus(dto.getStatus());
        }
        if (dto.getDailyLimit() != null) {
            account.setDailyLimit(dto.getDailyLimit());
        }
        if (dto.getRemark() != null) {
            account.setRemark(dto.getRemark());
        }

        accountMapper.updateById(account);
        return convertToVO(account);
    }

    /**
     * Get paginated account list with filters
     */
    public PageResult<AgentAccountVO> getAccountList(AgentAccountQueryDTO query) {
        Long tenantId = TenantContext.getTenantId();

        Page<AgentAccount> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<AgentAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentAccount::getTenantId, tenantId);

        if (StringUtils.hasText(query.getPlatform())) {
            wrapper.eq(AgentAccount::getPlatform, query.getPlatform());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(AgentAccount::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getAccountName())) {
            wrapper.like(AgentAccount::getAccountName, query.getAccountName());
        }

        wrapper.orderByDesc(AgentAccount::getCreatedAt);

        Page<AgentAccount> result = accountMapper.selectPage(page, wrapper);

        List<AgentAccountVO> voList = new ArrayList<>();
        for (AgentAccount account : result.getRecords()) {
            voList.add(convertToVO(account));
        }

        return new PageResult<>(result.getTotal(), voList, query.getPageNum(), query.getPageSize());
    }

    /**
     * Get account detail by ID
     */
    public AgentAccountVO getAccountDetail(Long id) {
        Long tenantId = TenantContext.getTenantId();

        AgentAccount account = accountMapper.selectById(id);
        if (account == null) {
            throw new BizException("Agent account not found");
        }
        if (!account.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        return convertToVO(account);
    }

    /**
     * Update account status
     */
    public AgentAccountVO updateStatus(Long id, String status) {
        Long tenantId = TenantContext.getTenantId();

        AgentAccount account = accountMapper.selectById(id);
        if (account == null) {
            throw new BizException("Agent account not found");
        }
        if (!account.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        account.setStatus(status);
        accountMapper.updateById(account);

        return convertToVO(account);
    }

    /**
     * Refresh health score based on recent activity
     */
    public AgentAccountVO refreshHealth(Long id) {
        Long tenantId = TenantContext.getTenantId();

        AgentAccount account = accountMapper.selectById(id);
        if (account == null) {
            throw new BizException("Agent account not found");
        }
        if (!account.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        // Calculate health score based on recent behavior logs
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last24h = now.minus(24, ChronoUnit.HOURS);

        LambdaQueryWrapper<AgentBehaviorLog> logWrapper = new LambdaQueryWrapper<>();
        logWrapper.eq(AgentBehaviorLog::getAgentAccountId, id)
                .ge(AgentBehaviorLog::getExecutedAt, last24h);

        List<AgentBehaviorLog> recentLogs = behaviorLogMapper.selectList(logWrapper);

        if (recentLogs.isEmpty()) {
            account.setHealthScore(100.0);
        } else {
            long successCount = 0;
            for (AgentBehaviorLog log : recentLogs) {
                if (log.getIsSuccess() != null && log.getIsSuccess() == 1) {
                    successCount++;
                }
            }
            double successRate = (double) successCount / recentLogs.size();
            // Health score: 70% based on success rate, 30% based on activity level
            double activityScore = Math.min(100.0, recentLogs.size() * 10.0);
            account.setHealthScore(Math.round((successRate * 70 + activityScore * 0.3) * 10.0) / 10.0);
        }

        accountMapper.updateById(account);
        return convertToVO(account);
    }

    /**
     * Convert AgentAccount entity to AgentAccountVO
     */
    private AgentAccountVO convertToVO(AgentAccount account) {
        AgentAccountVO vo = new AgentAccountVO();
        vo.setId(account.getId());
        vo.setTenantId(account.getTenantId());
        vo.setPlatform(account.getPlatform());
        vo.setAccountName(account.getAccountName());
        vo.setAccountId(account.getAccountId());
        vo.setStatus(account.getStatus());
        vo.setHealthScore(account.getHealthScore());
        vo.setDailyLimit(account.getDailyLimit());
        vo.setUsedToday(account.getUsedToday());
        vo.setLastActiveAt(account.getLastActiveAt());
        vo.setRemark(account.getRemark());
        vo.setCreatedBy(account.getCreatedBy());
        vo.setCreatedAt(account.getCreatedAt());
        vo.setUpdatedAt(account.getUpdatedAt());
        return vo;
    }
}
