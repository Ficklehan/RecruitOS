package com.recruitos.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.result.PageResult;
import com.recruitos.tenant.dto.TenantCreateDTO;
import com.recruitos.tenant.dto.TenantDetailVO;
import com.recruitos.tenant.entity.*;
import com.recruitos.tenant.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantManagementService {

    private final TenantMapper tenantMapper;
    private final TenantLicenseMapper tenantLicenseMapper;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final OrganizationMapper organizationMapper;

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private static final String[][] DEFAULT_ROLES = {
            {"SUPER_ADMIN", "超级管理员", "系统最高权限"},
            {"HR_MANAGER", "HR负责人", "HR部门负责人"},
            {"RECRUITER", "招聘HR", "招聘专员"},
            {"INTERVIEWER", "面试官", "面试官"},
            {"DEPT_HEAD", "部门负责人", "用人部门负责人"},
            {"SSC", "SSC专员", "共享服务中心"},
            {"EMPLOYEE", "员工", "普通员工(内推)"}
    };

    public PageResult<TenantDetailVO> listTenants(int pageNum, int pageSize, String keyword, Integer status) {
        Page<Tenant> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Tenant::getCompanyName, keyword)
                    .or().like(Tenant::getTenantCode, keyword));
        }
        if (status != null) {
            wrapper.eq(Tenant::getStatus, status);
        }
        wrapper.orderByDesc(Tenant::getCreatedAt);

        Page<Tenant> result = tenantMapper.selectPage(page, wrapper);
        List<TenantDetailVO> voList = new ArrayList<>();
        for (Tenant tenant : result.getRecords()) {
            voList.add(buildDetailVO(tenant));
        }
        return new PageResult<>(result.getTotal(), voList, pageNum, pageSize);
    }

    public TenantDetailVO getTenantDetail(Long id) {
        Tenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw new BizException(404, "租户不存在");
        }
        return buildDetailVO(tenant);
    }

    @Transactional(rollbackFor = Exception.class)
    public TenantDetailVO createTenantWithAdmin(TenantCreateDTO dto) {
        // Check duplicate tenant code
        LambdaQueryWrapper<Tenant> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(Tenant::getTenantCode, dto.getTenantCode());
        if (tenantMapper.selectCount(checkWrapper) > 0) {
            throw new BizException(400, "租户编码已存在");
        }

        // 1. Create tenant
        Tenant tenant = new Tenant();
        tenant.setTenantCode(dto.getTenantCode());
        tenant.setCompanyName(dto.getCompanyName());
        tenant.setCreditCode(dto.getCreditCode());
        tenant.setPlan(dto.getPlan() != null ? dto.getPlan() : "STARTER");
        tenant.setStatus(1);
        tenant.setTrialEndTime(LocalDateTime.now().plusDays(30));
        tenantMapper.insert(tenant);

        // 2. Create license
        PlanQuotaConfig quota = PlanQuotaConfig.fromPlan(tenant.getPlan());
        TenantLicense license = new TenantLicense();
        license.setTenantId(tenant.getId());
        license.setPlan(tenant.getPlan());
        license.setMaxJobs(quota.getMaxJobs());
        license.setMaxAgents(quota.getMaxAgents());
        license.setUsedJobs(0);
        license.setUsedAgents(0);
        license.setResumeQuota(quota.getResumeQuota());
        license.setResumeUsed(0);
        license.setMessageQuota(quota.getMessageQuota());
        license.setMessageUsed(0);
        license.setStartDate(LocalDate.now());
        license.setEndDate(LocalDate.now().plusYears(1));
        license.setGraceDays(15);
        license.setStatus(1);
        tenantLicenseMapper.insert(license);

        // 3. Create default organization
        Organization org = new Organization();
        org.setTenantId(tenant.getId());
        org.setParentId(0L);
        org.setName(dto.getCompanyName());
        org.setType("COMPANY");
        org.setSortOrder(0);
        org.setStatus(1);
        organizationMapper.insert(org);

        // 4. Create default roles
        List<Long> roleIds = new ArrayList<>();
        for (int i = 0; i < DEFAULT_ROLES.length; i++) {
            SysRole role = new SysRole();
            role.setTenantId(tenant.getId());
            role.setRoleCode(DEFAULT_ROLES[i][0]);
            role.setRoleName(DEFAULT_ROLES[i][1]);
            role.setDescription(DEFAULT_ROLES[i][2]);
            role.setSortOrder(i);
            role.setStatus(1);
            sysRoleMapper.insert(role);
            roleIds.add(role.getId());
        }

        // 5. Create admin user
        SysUser adminUser = new SysUser();
        adminUser.setTenantId(tenant.getId());
        adminUser.setUsername(dto.getAdminUsername() != null ? dto.getAdminUsername() : "admin");
        adminUser.setPasswordHash(ENCODER.encode(dto.getAdminPassword() != null ? dto.getAdminPassword() : "123"));
        adminUser.setRealName(dto.getAdminRealName() != null ? dto.getAdminRealName() : "管理员");
        adminUser.setEmail(dto.getAdminEmail());
        adminUser.setPhone(dto.getAdminPhone());
        adminUser.setOrgId(org.getId());
        adminUser.setStatus(1);
        sysUserMapper.insert(adminUser);

        // 6. Assign SUPER_ADMIN role to admin user
        SysUserRole userRole = new SysUserRole();
        userRole.setTenantId(tenant.getId());
        userRole.setUserId(adminUser.getId());
        userRole.setRoleId(roleIds.get(0)); // SUPER_ADMIN
        userRole.setScopeType("ALL");
        sysUserRoleMapper.insert(userRole);

        return buildDetailVO(tenant);
    }

    public TenantDetailVO updateTenantStatus(Long id, Integer status) {
        Tenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw new BizException(404, "租户不存在");
        }
        tenant.setStatus(status);
        tenantMapper.updateById(tenant);
        return buildDetailVO(tenant);
    }

    public TenantDetailVO updateTenantPlan(Long id, String newPlan) {
        Tenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw new BizException(404, "租户不存在");
        }

        // Validate plan
        try {
            PlanQuotaConfig.valueOf(newPlan.toUpperCase());
        } catch (Exception e) {
            throw new BizException(400, "无效的套餐类型");
        }

        tenant.setPlan(newPlan.toUpperCase());
        tenantMapper.updateById(tenant);

        // Update license quotas
        LambdaQueryWrapper<TenantLicense> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantLicense::getTenantId, id);
        TenantLicense license = tenantLicenseMapper.selectOne(wrapper);
        if (license != null) {
            PlanQuotaConfig quota = PlanQuotaConfig.fromPlan(newPlan);
            license.setPlan(newPlan.toUpperCase());
            license.setMaxJobs(quota.getMaxJobs());
            license.setMaxAgents(quota.getMaxAgents());
            license.setResumeQuota(quota.getResumeQuota());
            license.setMessageQuota(quota.getMessageQuota());
            tenantLicenseMapper.updateById(license);
        }

        return buildDetailVO(tenant);
    }

    public TenantLicense getTenantLicense(Long tenantId) {
        LambdaQueryWrapper<TenantLicense> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantLicense::getTenantId, tenantId);
        return tenantLicenseMapper.selectOne(wrapper);
    }

    public TenantLicense updateTenantLicense(Long tenantId, TenantLicense update) {
        LambdaQueryWrapper<TenantLicense> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantLicense::getTenantId, tenantId);
        TenantLicense license = tenantLicenseMapper.selectOne(wrapper);
        if (license == null) {
            throw new BizException(404, "License不存在");
        }
        if (update.getMaxJobs() != null) license.setMaxJobs(update.getMaxJobs());
        if (update.getMaxAgents() != null) license.setMaxAgents(update.getMaxAgents());
        if (update.getResumeQuota() != null) license.setResumeQuota(update.getResumeQuota());
        if (update.getMessageQuota() != null) license.setMessageQuota(update.getMessageQuota());
        if (update.getEndDate() != null) license.setEndDate(update.getEndDate());
        if (update.getGraceDays() != null) license.setGraceDays(update.getGraceDays());
        tenantLicenseMapper.updateById(license);
        return license;
    }

    private TenantDetailVO buildDetailVO(Tenant tenant) {
        TenantDetailVO vo = new TenantDetailVO();
        vo.setId(tenant.getId());
        vo.setTenantCode(tenant.getTenantCode());
        vo.setCompanyName(tenant.getCompanyName());
        vo.setCreditCode(tenant.getCreditCode());
        vo.setPlan(tenant.getPlan());
        vo.setStatus(tenant.getStatus());
        vo.setTrialEndTime(tenant.getTrialEndTime());
        vo.setCreatedAt(tenant.getCreatedAt());

        // Load license
        LambdaQueryWrapper<TenantLicense> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantLicense::getTenantId, tenant.getId());
        TenantLicense license = tenantLicenseMapper.selectOne(wrapper);
        if (license != null) {
            vo.setMaxJobs(license.getMaxJobs());
            vo.setMaxAgents(license.getMaxAgents());
            vo.setUsedJobs(license.getUsedJobs());
            vo.setUsedAgents(license.getUsedAgents());
            vo.setResumeQuota(license.getResumeQuota());
            vo.setResumeUsed(license.getResumeUsed());
            vo.setMessageQuota(license.getMessageQuota());
            vo.setMessageUsed(license.getMessageUsed());
            vo.setLicenseStartDate(license.getStartDate());
            vo.setLicenseEndDate(license.getEndDate());
            vo.setGraceDays(license.getGraceDays());
            vo.setLicenseStatus(license.getStatus());
        }
        return vo;
    }
}
