package com.recruitos.headhunter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruitos.common.auth.CurrentUser;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.headhunter.dto.VendorCreateDTO;
import com.recruitos.headhunter.dto.VendorQueryDTO;
import com.recruitos.headhunter.dto.VendorVO;
import com.recruitos.headhunter.entity.HeadhunterVendor;
import com.recruitos.headhunter.mapper.HeadhunterVendorMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vendor service - business logic for headhunter vendor management
 */
@Service
public class VendorService {

    @Resource
    private HeadhunterVendorMapper vendorMapper;

    /**
     * Create a new headhunter vendor
     */
    @Transactional
    public VendorVO createVendor(VendorCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = CurrentUser.getCurrentUserId();
        if (tenantId == null || userId == null) {
            throw new BizException("User not authenticated");
        }

        HeadhunterVendor vendor = new HeadhunterVendor();
        vendor.setTenantId(tenantId);
        vendor.setVendorName(dto.getVendorName());
        vendor.setContactPerson(dto.getContactPerson());
        vendor.setContactPhone(dto.getContactPhone());
        vendor.setContactEmail(dto.getContactEmail());
        vendor.setContractStart(dto.getContractStart());
        vendor.setContractEnd(dto.getContractEnd());
        vendor.setCommissionRate(dto.getCommissionRate() != null ? java.math.BigDecimal.valueOf(dto.getCommissionRate()) : null);
        vendor.setStatus(1); // 1=ACTIVE
        vendor.setTotalRecommendations(0);
        vendor.setSuccessfulHires(0);

        vendorMapper.insert(vendor);

        return convertToVO(vendor);
    }

    /**
     * Update an existing vendor
     */
    @Transactional
    public VendorVO updateVendor(Long vendorId, VendorCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();

        HeadhunterVendor vendor = vendorMapper.selectById(vendorId);
        if (vendor == null) {
            throw new BizException("Vendor not found");
        }
        if (!vendor.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        vendor.setVendorName(dto.getVendorName());
        vendor.setContactPerson(dto.getContactPerson());
        vendor.setContactPhone(dto.getContactPhone());
        vendor.setContactEmail(dto.getContactEmail());
        vendor.setContractStart(dto.getContractStart());
        vendor.setContractEnd(dto.getContractEnd());
        vendor.setCommissionRate(dto.getCommissionRate() != null ? java.math.BigDecimal.valueOf(dto.getCommissionRate()) : null);

        // Auto-update status based on contract dates
        if (dto.getContractEnd() != null && dto.getContractEnd().isBefore(LocalDate.now())) {
            vendor.setStatus(2); // 2=EXPIRED
        }

        vendorMapper.updateById(vendor);

        return convertToVO(vendor);
    }

    /**
     * Get vendor list with pagination and filters
     */
    public PageResult<VendorVO> getVendorList(VendorQueryDTO query) {
        Long tenantId = TenantContext.getTenantId();

        Page<HeadhunterVendor> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<HeadhunterVendor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HeadhunterVendor::getTenantId, tenantId);

        if (StringUtils.hasText(query.getVendorName())) {
            wrapper.like(HeadhunterVendor::getVendorName, query.getVendorName());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(HeadhunterVendor::getStatus, Integer.parseInt(query.getStatus()));
        }

        wrapper.orderByDesc(HeadhunterVendor::getCreatedAt);

        Page<HeadhunterVendor> result = vendorMapper.selectPage(page, wrapper);

        List<VendorVO> voList = new ArrayList<>();
        for (HeadhunterVendor vendor : result.getRecords()) {
            voList.add(convertToVO(vendor));
        }

        return new PageResult<>(result.getTotal(), voList, query.getPageNum(), query.getPageSize());
    }

    /**
     * Get vendor detail by ID
     */
    public VendorVO getVendorDetail(Long vendorId) {
        Long tenantId = TenantContext.getTenantId();

        HeadhunterVendor vendor = vendorMapper.selectById(vendorId);
        if (vendor == null) {
            throw new BizException("Vendor not found");
        }
        if (!vendor.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        return convertToVO(vendor);
    }

    /**
     * Delete a vendor (soft delete or set status to INACTIVE)
     */
    @Transactional
    public void deleteVendor(Long vendorId) {
        Long tenantId = TenantContext.getTenantId();

        HeadhunterVendor vendor = vendorMapper.selectById(vendorId);
        if (vendor == null) {
            throw new BizException("Vendor not found");
        }
        if (!vendor.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        vendor.setStatus(0); // 0=INACTIVE
        vendorMapper.updateById(vendor);
    }

    /**
     * Get vendor statistics
     */
    public Map<String, Object> getVendorStats() {
        Long tenantId = TenantContext.getTenantId();

        Map<String, Object> stats = new HashMap<>();

        // Total vendors
        LambdaQueryWrapper<HeadhunterVendor> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(HeadhunterVendor::getTenantId, tenantId);
        long totalVendors = vendorMapper.selectCount(totalWrapper);
        stats.put("totalVendors", totalVendors);

        // Active vendors
        LambdaQueryWrapper<HeadhunterVendor> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.eq(HeadhunterVendor::getTenantId, tenantId)
                .eq(HeadhunterVendor::getStatus, 1);
        long activeVendors = vendorMapper.selectCount(activeWrapper);
        stats.put("activeVendors", activeVendors);

        // Expired vendors
        LambdaQueryWrapper<HeadhunterVendor> expiredWrapper = new LambdaQueryWrapper<>();
        expiredWrapper.eq(HeadhunterVendor::getTenantId, tenantId)
                .eq(HeadhunterVendor::getStatus, 2);
        long expiredVendors = vendorMapper.selectCount(expiredWrapper);
        stats.put("expiredVendors", expiredVendors);

        // Aggregate stats
        LambdaQueryWrapper<HeadhunterVendor> allWrapper = new LambdaQueryWrapper<>();
        allWrapper.eq(HeadhunterVendor::getTenantId, tenantId);
        List<HeadhunterVendor> allVendors = vendorMapper.selectList(allWrapper);

        int totalRecommendations = 0;
        int totalSuccessfulHires = 0;
        for (HeadhunterVendor vendor : allVendors) {
            totalRecommendations += vendor.getTotalRecommendations() != null ? vendor.getTotalRecommendations() : 0;
            totalSuccessfulHires += vendor.getSuccessfulHires() != null ? vendor.getSuccessfulHires() : 0;
        }
        stats.put("totalRecommendations", totalRecommendations);
        stats.put("totalSuccessfulHires", totalSuccessfulHires);

        // Conversion rate
        double conversionRate = totalRecommendations > 0 ? (double) totalSuccessfulHires / totalRecommendations * 100 : 0.0;
        stats.put("conversionRate", Math.round(conversionRate * 100.0) / 100.0);

        return stats;
    }

    /**
     * Convert entity to VO
     */
    private VendorVO convertToVO(HeadhunterVendor vendor) {
        VendorVO vo = new VendorVO();
        vo.setId(vendor.getId());
        vo.setTenantId(vendor.getTenantId());
        vo.setVendorName(vendor.getVendorName());
        vo.setContactPerson(vendor.getContactPerson());
        vo.setContactPhone(vendor.getContactPhone());
        vo.setContactEmail(vendor.getContactEmail());
        vo.setContractStart(vendor.getContractStart());
        vo.setContractEnd(vendor.getContractEnd());
        vo.setCommissionRate(vendor.getCommissionRate() != null ? vendor.getCommissionRate().doubleValue() : null);
        vo.setStatus(vendor.getStatus() != null ? vendor.getStatus().toString() : null);
        vo.setTotalRecommendations(vendor.getTotalRecommendations());
        vo.setSuccessfulHires(vendor.getSuccessfulHires());
        vo.setCreatedAt(vendor.getCreatedAt());
        vo.setUpdatedAt(vendor.getUpdatedAt());
        return vo;
    }
}
