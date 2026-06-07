package com.recruitos.referral.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruitos.common.auth.CurrentUser;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.referral.dto.ReferralCreateDTO;
import com.recruitos.referral.dto.ReferralQueryDTO;
import com.recruitos.referral.dto.ReferralVO;
import com.recruitos.referral.entity.Referral;
import com.recruitos.referral.mapper.ReferralMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * Referral service - core business logic for internal referral management
 */
@Service
public class ReferralService {

    @Resource
    private ReferralMapper referralMapper;

    /**
     * Create a new referral
     */
    @Transactional
    public ReferralVO createReferral(ReferralCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = CurrentUser.getCurrentUserId();
        if (tenantId == null || userId == null) {
            throw new BizException("User not authenticated");
        }

        Referral referral = new Referral();
        referral.setTenantId(tenantId);
        referral.setReferrerId(dto.getReferrerId());
        referral.setReferrerName(dto.getReferrerName());
        referral.setCandidateId(dto.getCandidateId());
        referral.setCandidateName(dto.getCandidateName());
        referral.setJobId(dto.getJobId());
        referral.setJobTitle(dto.getJobTitle());
        referral.setStatus("PENDING");
        referral.setRemark(dto.getRemark());
        referral.setCreatedBy(userId);

        referralMapper.insert(referral);

        return convertToVO(referral);
    }

    /**
     * Get referral list with pagination and filters
     */
    public PageResult<ReferralVO> getReferralList(ReferralQueryDTO query) {
        Long tenantId = TenantContext.getTenantId();

        Page<Referral> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<Referral> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Referral::getTenantId, tenantId);

        if (query.getReferrerId() != null) {
            wrapper.eq(Referral::getReferrerId, query.getReferrerId());
        }
        if (StringUtils.hasText(query.getCandidateName())) {
            wrapper.like(Referral::getCandidateName, query.getCandidateName());
        }
        if (StringUtils.hasText(query.getJobTitle())) {
            wrapper.like(Referral::getJobTitle, query.getJobTitle());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Referral::getStatus, query.getStatus());
        }

        wrapper.orderByDesc(Referral::getCreatedAt);

        Page<Referral> result = referralMapper.selectPage(page, wrapper);

        List<ReferralVO> voList = new ArrayList<>();
        for (Referral referral : result.getRecords()) {
            voList.add(convertToVO(referral));
        }

        return new PageResult<>(result.getTotal(), voList, query.getPageNum(), query.getPageSize());
    }

    /**
     * Get referral detail by ID
     */
    public ReferralVO getReferralDetail(Long referralId) {
        Long tenantId = TenantContext.getTenantId();

        Referral referral = referralMapper.selectById(referralId);
        if (referral == null) {
            throw new BizException("Referral not found");
        }
        if (!referral.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        return convertToVO(referral);
    }

    /**
     * Update referral status
     */
    @Transactional
    public ReferralVO updateStatus(Long referralId, String status) {
        Long tenantId = TenantContext.getTenantId();

        Referral referral = referralMapper.selectById(referralId);
        if (referral == null) {
            throw new BizException("Referral not found");
        }
        if (!referral.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        referral.setStatus(status);
        referralMapper.updateById(referral);

        return convertToVO(referral);
    }

    /**
     * Convert entity to VO
     */
    private ReferralVO convertToVO(Referral referral) {
        ReferralVO vo = new ReferralVO();
        vo.setId(referral.getId());
        vo.setTenantId(referral.getTenantId());
        vo.setReferrerId(referral.getReferrerId());
        vo.setReferrerName(referral.getReferrerName());
        vo.setCandidateId(referral.getCandidateId());
        vo.setCandidateName(referral.getCandidateName());
        vo.setJobId(referral.getJobId());
        vo.setJobTitle(referral.getJobTitle());
        vo.setStatus(referral.getStatus());
        vo.setRemark(referral.getRemark());
        vo.setCreatedBy(referral.getCreatedBy());
        vo.setCreatedAt(referral.getCreatedAt());
        vo.setUpdatedAt(referral.getUpdatedAt());
        return vo;
    }
}
