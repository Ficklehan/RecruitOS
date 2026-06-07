package com.recruitos.referral.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruitos.common.auth.CurrentUser;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.referral.dto.RewardCreateDTO;
import com.recruitos.referral.dto.RewardQueryDTO;
import com.recruitos.referral.dto.RewardVO;
import com.recruitos.referral.entity.ReferralReward;
import com.recruitos.referral.mapper.ReferralRewardMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Referral reward service - business logic for referral reward management
 */
@Service
public class ReferralRewardService {

    @Resource
    private ReferralRewardMapper referralRewardMapper;

    /**
     * Create a new referral reward
     */
    @Transactional
    public RewardVO createReward(RewardCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BizException("User not authenticated");
        }

        ReferralReward reward = new ReferralReward();
        reward.setTenantId(tenantId);
        reward.setReferralId(dto.getReferralId());
        reward.setReferrerId(dto.getReferrerId());
        reward.setReferrerName(dto.getReferrerName());
        reward.setRewardType(dto.getRewardType());
        reward.setRewardAmount(dto.getRewardAmount());
        reward.setStatus("PENDING");
        reward.setRemark(dto.getRemark());

        referralRewardMapper.insert(reward);

        return convertToVO(reward);
    }

    /**
     * Get reward list with pagination and filters
     */
    public PageResult<RewardVO> getRewardList(RewardQueryDTO query) {
        Long tenantId = TenantContext.getTenantId();

        Page<ReferralReward> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<ReferralReward> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReferralReward::getTenantId, tenantId);

        if (query.getReferralId() != null) {
            wrapper.eq(ReferralReward::getReferralId, query.getReferralId());
        }
        if (query.getReferrerId() != null) {
            wrapper.eq(ReferralReward::getReferrerId, query.getReferrerId());
        }
        if (StringUtils.hasText(query.getRewardType())) {
            wrapper.eq(ReferralReward::getRewardType, query.getRewardType());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(ReferralReward::getStatus, query.getStatus());
        }

        wrapper.orderByDesc(ReferralReward::getCreatedAt);

        Page<ReferralReward> result = referralRewardMapper.selectPage(page, wrapper);

        List<RewardVO> voList = new ArrayList<>();
        for (ReferralReward reward : result.getRecords()) {
            voList.add(convertToVO(reward));
        }

        return new PageResult<>(result.getTotal(), voList, query.getPageNum(), query.getPageSize());
    }

    /**
     * Approve a reward
     */
    @Transactional
    public RewardVO approveReward(Long rewardId) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = CurrentUser.getCurrentUserId();

        ReferralReward reward = referralRewardMapper.selectById(rewardId);
        if (reward == null) {
            throw new BizException("Reward not found");
        }
        if (!reward.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if (!"PENDING".equals(reward.getStatus())) {
            throw new BizException("Only PENDING rewards can be approved");
        }

        reward.setStatus("APPROVED");
        reward.setApprovedBy(userId);
        reward.setApprovedAt(LocalDateTime.now());
        referralRewardMapper.updateById(reward);

        return convertToVO(reward);
    }

    /**
     * Mark a reward as paid
     */
    @Transactional
    public RewardVO payReward(Long rewardId) {
        Long tenantId = TenantContext.getTenantId();

        ReferralReward reward = referralRewardMapper.selectById(rewardId);
        if (reward == null) {
            throw new BizException("Reward not found");
        }
        if (!reward.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if (!"APPROVED".equals(reward.getStatus())) {
            throw new BizException("Only APPROVED rewards can be paid");
        }

        reward.setStatus("PAID");
        reward.setPaidAt(LocalDateTime.now());
        referralRewardMapper.updateById(reward);

        return convertToVO(reward);
    }

    /**
     * Get reward statistics
     */
    public Map<String, Object> getRewardStats() {
        Long tenantId = TenantContext.getTenantId();

        Map<String, Object> stats = new HashMap<>();

        // Total rewards
        LambdaQueryWrapper<ReferralReward> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(ReferralReward::getTenantId, tenantId);
        long totalRewards = referralRewardMapper.selectCount(totalWrapper);
        stats.put("totalRewards", totalRewards);

        // Pending rewards
        LambdaQueryWrapper<ReferralReward> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(ReferralReward::getTenantId, tenantId)
                .eq(ReferralReward::getStatus, "PENDING");
        long pendingRewards = referralRewardMapper.selectCount(pendingWrapper);
        stats.put("pendingRewards", pendingRewards);

        // Approved rewards
        LambdaQueryWrapper<ReferralReward> approvedWrapper = new LambdaQueryWrapper<>();
        approvedWrapper.eq(ReferralReward::getTenantId, tenantId)
                .eq(ReferralReward::getStatus, "APPROVED");
        long approvedRewards = referralRewardMapper.selectCount(approvedWrapper);
        stats.put("approvedRewards", approvedRewards);

        // Paid rewards
        LambdaQueryWrapper<ReferralReward> paidWrapper = new LambdaQueryWrapper<>();
        paidWrapper.eq(ReferralReward::getTenantId, tenantId)
                .eq(ReferralReward::getStatus, "PAID");
        long paidRewards = referralRewardMapper.selectCount(paidWrapper);
        stats.put("paidRewards", paidRewards);

        // Total paid amount
        LambdaQueryWrapper<ReferralReward> paidAmountWrapper = new LambdaQueryWrapper<>();
        paidAmountWrapper.eq(ReferralReward::getTenantId, tenantId)
                .eq(ReferralReward::getStatus, "PAID");
        List<ReferralReward> paidRewardsList = referralRewardMapper.selectList(paidAmountWrapper);
        double totalPaidAmount = 0.0;
        for (ReferralReward reward : paidRewardsList) {
            totalPaidAmount += reward.getRewardAmount() != null ? reward.getRewardAmount() : 0.0;
        }
        stats.put("totalPaidAmount", totalPaidAmount);

        return stats;
    }

    /**
     * Convert entity to VO
     */
    private RewardVO convertToVO(ReferralReward reward) {
        RewardVO vo = new RewardVO();
        vo.setId(reward.getId());
        vo.setTenantId(reward.getTenantId());
        vo.setReferralId(reward.getReferralId());
        vo.setReferrerId(reward.getReferrerId());
        vo.setReferrerName(reward.getReferrerName());
        vo.setRewardType(reward.getRewardType());
        vo.setRewardAmount(reward.getRewardAmount());
        vo.setStatus(reward.getStatus());
        vo.setApprovedBy(reward.getApprovedBy());
        vo.setApprovedAt(reward.getApprovedAt());
        vo.setPaidAt(reward.getPaidAt());
        vo.setRemark(reward.getRemark());
        vo.setCreatedAt(reward.getCreatedAt());
        vo.setUpdatedAt(reward.getUpdatedAt());
        return vo;
    }
}
