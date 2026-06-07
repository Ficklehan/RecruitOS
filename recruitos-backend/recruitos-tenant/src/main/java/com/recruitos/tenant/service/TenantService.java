package com.recruitos.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruitos.common.exception.BizException;
import com.recruitos.tenant.entity.Tenant;
import com.recruitos.tenant.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Tenant service with CRUD and plan-based validation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantMapper tenantMapper;

    /**
     * Page query tenants
     */
    public IPage<Tenant> page(int pageNum, int pageSize, String keyword) {
        Page<Tenant> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Tenant::getCompanyName, keyword)
                    .or()
                    .like(Tenant::getTenantCode, keyword);
        }
        wrapper.orderByDesc(Tenant::getCreatedAt);
        return tenantMapper.selectPage(page, wrapper);
    }

    /**
     * Get tenant by ID
     */
    public Tenant getById(Long id) {
        Tenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw new BizException(404, "Tenant not found");
        }
        return tenant;
    }

    /**
     * Create tenant with plan validation
     */
    public Tenant create(Tenant tenant) {
        // Check duplicate tenant code
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tenant::getTenantCode, tenant.getTenantCode());
        if (tenantMapper.selectCount(wrapper) > 0) {
            throw new BizException(400, "Tenant code already exists");
        }
        // Set default status
        if (tenant.getStatus() == null) {
            tenant.setStatus(1);
        }
        // Validate plan
        validatePlan(tenant.getPlan());
        tenantMapper.insert(tenant);
        return tenant;
    }

    /**
     * Update tenant
     */
    public Tenant update(Long id, Tenant tenant) {
        Tenant existing = getById(id);
        existing.setCompanyName(tenant.getCompanyName());
        existing.setCreditCode(tenant.getCreditCode());
        existing.setPlan(tenant.getPlan());
        existing.setStatus(tenant.getStatus());
        existing.setTrialEndTime(tenant.getTrialEndTime());
        existing.setConfigJson(tenant.getConfigJson());
        validatePlan(existing.getPlan());
        tenantMapper.updateById(existing);
        return existing;
    }

    /**
     * Delete tenant
     */
    public void delete(Long id) {
        getById(id);
        tenantMapper.deleteById(id);
    }

    /**
     * List all active tenants (no pagination, for login dropdown)
     */
    public List<Tenant> listAll() {
        return tenantMapper.selectList(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getStatus, 1)
                .orderByAsc(Tenant::getId));
    }

    /**
     * Validate plan type
     */
    private void validatePlan(String plan) {
        if (!StringUtils.hasText(plan)) {
            return;
        }
        if (!"STARTER".equals(plan) && !"BASIC".equals(plan)
                && !"PRO".equals(plan) && !"ENTERPRISE".equals(plan)) {
            throw new BizException(400, "Invalid plan type. Must be one of: STARTER, BASIC, PRO, ENTERPRISE");
        }
    }
}
