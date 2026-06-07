package com.recruitos.headhunter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruitos.common.auth.CurrentUser;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.headhunter.dto.RecommendationCreateDTO;
import com.recruitos.headhunter.dto.RecommendationQueryDTO;
import com.recruitos.headhunter.dto.RecommendationVO;
import com.recruitos.headhunter.entity.HeadhunterRecommendation;
import com.recruitos.headhunter.entity.HeadhunterVendor;
import com.recruitos.headhunter.mapper.HeadhunterRecommendationMapper;
import com.recruitos.headhunter.mapper.HeadhunterVendorMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Recommendation service - business logic for headhunter recommendation management
 */
@Service
public class RecommendationService {

    @Resource
    private HeadhunterRecommendationMapper recommendationMapper;

    @Resource
    private HeadhunterVendorMapper vendorMapper;

    /**
     * Create a new recommendation
     */
    @Transactional
    public RecommendationVO createRecommendation(RecommendationCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = CurrentUser.getCurrentUserId();
        if (tenantId == null || userId == null) {
            throw new BizException("User not authenticated");
        }

        // Verify vendor exists
        HeadhunterVendor vendor = vendorMapper.selectById(dto.getVendorId());
        if (vendor == null) {
            throw new BizException("Vendor not found");
        }

        HeadhunterRecommendation recommendation = new HeadhunterRecommendation();
        recommendation.setTenantId(tenantId);
        recommendation.setVendorId(dto.getVendorId());
        recommendation.setCandidateId(dto.getCandidateId());
        recommendation.setJobId(dto.getJobId());
        recommendation.setStatus("SUBMITTED");
        if (dto.getCommissionAmount() != null) {
            recommendation.setFeeAmount(java.math.BigDecimal.valueOf(dto.getCommissionAmount()));
        }

        recommendationMapper.insert(recommendation);

        // Update vendor total recommendations count
        vendor.setTotalRecommendations((vendor.getTotalRecommendations() != null ? vendor.getTotalRecommendations() : 0) + 1);
        vendorMapper.updateById(vendor);

        return convertToVO(recommendation);
    }

    /**
     * Get recommendation list with pagination and filters
     */
    public PageResult<RecommendationVO> getRecommendationList(RecommendationQueryDTO query) {
        Long tenantId = TenantContext.getTenantId();

        Page<HeadhunterRecommendation> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<HeadhunterRecommendation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HeadhunterRecommendation::getTenantId, tenantId);

        if (query.getVendorId() != null) {
            wrapper.eq(HeadhunterRecommendation::getVendorId, query.getVendorId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(HeadhunterRecommendation::getStatus, query.getStatus());
        }

        wrapper.orderByDesc(HeadhunterRecommendation::getCreatedAt);

        Page<HeadhunterRecommendation> result = recommendationMapper.selectPage(page, wrapper);

        List<RecommendationVO> voList = new ArrayList<>();
        for (HeadhunterRecommendation recommendation : result.getRecords()) {
            voList.add(convertToVO(recommendation));
        }

        return new PageResult<>(result.getTotal(), voList, query.getPageNum(), query.getPageSize());
    }

    /**
     * Update recommendation status
     */
    @Transactional
    public RecommendationVO updateStatus(Long recommendationId, String status) {
        Long tenantId = TenantContext.getTenantId();

        HeadhunterRecommendation recommendation = recommendationMapper.selectById(recommendationId);
        if (recommendation == null) {
            throw new BizException("Recommendation not found");
        }
        if (!recommendation.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        String oldStatus = recommendation.getStatus();
        recommendation.setStatus(status);
        recommendationMapper.updateById(recommendation);

        // If status changed to HIRED, update vendor successful hires count
        if ("HIRED".equals(status) && !"HIRED".equals(oldStatus)) {
            HeadhunterVendor vendor = vendorMapper.selectById(recommendation.getVendorId());
            if (vendor != null) {
                vendor.setSuccessfulHires((vendor.getSuccessfulHires() != null ? vendor.getSuccessfulHires() : 0) + 1);
                vendorMapper.updateById(vendor);
            }
        }

        return convertToVO(recommendation);
    }

    /**
     * Get recommendation statistics
     */
    public Map<String, Object> getRecommendationStats() {
        Long tenantId = TenantContext.getTenantId();

        Map<String, Object> stats = new HashMap<>();

        // Total recommendations
        LambdaQueryWrapper<HeadhunterRecommendation> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(HeadhunterRecommendation::getTenantId, tenantId);
        long totalRecommendations = recommendationMapper.selectCount(totalWrapper);
        stats.put("totalRecommendations", totalRecommendations);

        // By status
        String[] statusList = {"SUBMITTED", "INTERVIEWING", "OFFER", "HIRED", "REJECTED"};
        for (String status : statusList) {
            LambdaQueryWrapper<HeadhunterRecommendation> statusWrapper = new LambdaQueryWrapper<>();
            statusWrapper.eq(HeadhunterRecommendation::getTenantId, tenantId)
                    .eq(HeadhunterRecommendation::getStatus, status);
            long count = recommendationMapper.selectCount(statusWrapper);
            stats.put(status.toLowerCase() + "Count", count);
        }

        // Conversion rate (HIRED / total)
        LambdaQueryWrapper<HeadhunterRecommendation> hiredWrapper = new LambdaQueryWrapper<>();
        hiredWrapper.eq(HeadhunterRecommendation::getTenantId, tenantId)
                .eq(HeadhunterRecommendation::getStatus, "HIRED");
        long hiredCount = recommendationMapper.selectCount(hiredWrapper);
        double conversionRate = totalRecommendations > 0 ? (double) hiredCount / totalRecommendations * 100 : 0.0;
        stats.put("conversionRate", Math.round(conversionRate * 100.0) / 100.0);

        // Total commission amount
        LambdaQueryWrapper<HeadhunterRecommendation> commissionWrapper = new LambdaQueryWrapper<>();
        commissionWrapper.eq(HeadhunterRecommendation::getTenantId, tenantId)
                .eq(HeadhunterRecommendation::getStatus, "HIRED");
        List<HeadhunterRecommendation> hiredList = recommendationMapper.selectList(commissionWrapper);
        double totalCommission = 0.0;
        for (HeadhunterRecommendation rec : hiredList) {
            totalCommission += rec.getFeeAmount() != null ? rec.getFeeAmount().doubleValue() : 0.0;
        }
        stats.put("totalCommission", totalCommission);

        return stats;
    }

    /**
     * Convert entity to VO
     */
    private RecommendationVO convertToVO(HeadhunterRecommendation recommendation) {
        RecommendationVO vo = new RecommendationVO();
        vo.setId(recommendation.getId());
        vo.setTenantId(recommendation.getTenantId());
        vo.setVendorId(recommendation.getVendorId());
        vo.setCandidateId(recommendation.getCandidateId());
        vo.setJobId(recommendation.getJobId());
        vo.setStatus(recommendation.getStatus());
        vo.setCommissionAmount(recommendation.getFeeAmount() != null ? recommendation.getFeeAmount().doubleValue() : null);
        vo.setCreatedAt(recommendation.getCreatedAt());
        vo.setUpdatedAt(recommendation.getUpdatedAt());
        return vo;
    }
}
