package com.recruitos.referral.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.recruitos.common.auth.CurrentUser;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.referral.dto.ReferralLinkCreateDTO;
import com.recruitos.referral.dto.ReferralLinkVO;
import com.recruitos.referral.dto.ReferralPublicSubmitDTO;
import com.recruitos.referral.dto.ReferralVO;
import com.recruitos.referral.entity.Referral;
import com.recruitos.referral.entity.ReferralShareLink;
import com.recruitos.referral.mapper.ReferralCandidateWriteMapper;
import com.recruitos.referral.mapper.ReferralMapper;
import com.recruitos.referral.mapper.ReferralShareLinkMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ReferralShareService {

    @Resource
    private ReferralShareLinkMapper shareLinkMapper;
    @Resource
    private ReferralMapper referralMapper;
    @Resource
    private ReferralCandidateWriteMapper candidateWriteMapper;

    @Transactional
    public ReferralLinkVO createShareLink(ReferralLinkCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = CurrentUser.getCurrentUserId();
        if (tenantId == null || userId == null) {
            throw new BizException("User not authenticated");
        }

        String jobTitle = candidateWriteMapper.selectJobTitle(tenantId, dto.getJobId());
        if (!StringUtils.hasText(jobTitle)) {
            throw new BizException("Job not found");
        }

        Long referrerId = dto.getReferrerId() != null ? dto.getReferrerId() : userId;
        String referrerName = dto.getReferrerName();
        if (!StringUtils.hasText(referrerName)) {
            CurrentUser cu = CurrentUser.get();
            referrerName = cu != null && StringUtils.hasText(cu.getRealName())
                    ? cu.getRealName() : (cu != null ? cu.getUsername() : "员工");
        }

        ReferralShareLink link = new ReferralShareLink();
        link.setTenantId(tenantId);
        link.setToken(UUID.randomUUID().toString().replace("-", ""));
        link.setJobId(dto.getJobId());
        link.setJobTitle(jobTitle);
        link.setReferrerId(referrerId);
        link.setReferrerName(referrerName);
        link.setExpiresAt(LocalDateTime.now().plusDays(90));
        link.setCreatedAt(LocalDateTime.now());
        shareLinkMapper.insert(link);

        ReferralLinkVO vo = new ReferralLinkVO();
        vo.setToken(link.getToken());
        vo.setUrl("/referral/submit/" + link.getToken());
        vo.setJobId(link.getJobId());
        vo.setJobTitle(link.getJobTitle());
        vo.setReferrerId(link.getReferrerId());
        vo.setReferrerName(link.getReferrerName());
        return vo;
    }

    public ReferralLinkVO getLinkInfo(String token) {
        ReferralShareLink link = requireValidLink(token);
        ReferralLinkVO vo = new ReferralLinkVO();
        vo.setToken(link.getToken());
        vo.setJobId(link.getJobId());
        vo.setJobTitle(link.getJobTitle());
        vo.setReferrerId(link.getReferrerId());
        vo.setReferrerName(link.getReferrerName());
        return vo;
    }

    @Transactional
    public ReferralVO submitByToken(ReferralPublicSubmitDTO dto) {
        ReferralShareLink link = requireValidLink(dto.getToken());
        TenantContext.setTenantId(link.getTenantId());

        Long candidateId = IdWorker.getId();
        Long candidateJobId = IdWorker.getId();
        String sourceDetail = "referral-link:" + link.getToken();
        candidateWriteMapper.insertCandidate(candidateId, link.getTenantId(), dto.getCandidateName(),
                dto.getPhone(), dto.getEmail(), sourceDetail);
        candidateWriteMapper.insertCandidateJob(candidateJobId, link.getTenantId(), candidateId, link.getJobId());

        Referral referral = new Referral();
        referral.setTenantId(link.getTenantId());
        referral.setReferrerId(link.getReferrerId());
        referral.setReferrerName(link.getReferrerName());
        referral.setCandidateId(candidateId);
        referral.setCandidateName(dto.getCandidateName());
        referral.setJobId(link.getJobId());
        referral.setJobTitle(link.getJobTitle());
        referral.setStatus("SUBMITTED");
        referral.setRemark(dto.getRemark());
        referral.setCreatedBy(link.getReferrerId());
        referralMapper.insert(referral);

        ReferralVO vo = new ReferralVO();
        vo.setId(referral.getId());
        vo.setCandidateId(candidateId);
        vo.setCandidateName(dto.getCandidateName());
        vo.setJobId(link.getJobId());
        vo.setJobTitle(link.getJobTitle());
        vo.setReferrerId(link.getReferrerId());
        vo.setReferrerName(link.getReferrerName());
        vo.setStatus("SUBMITTED");
        return vo;
    }

    @Transactional
    public void onOfferAccepted(Long tenantId, Long candidateId) {
        LambdaQueryWrapper<Referral> w = new LambdaQueryWrapper<>();
        w.eq(Referral::getTenantId, tenantId).eq(Referral::getCandidateId, candidateId).last("LIMIT 1");
        Referral referral = referralMapper.selectOne(w);
        if (referral == null) {
            return;
        }
        referral.setStatus("INTERVIEWING");
        referral.setRewardStatus("PENDING");
        referralMapper.updateById(referral);
    }

    @Transactional
    public void onOnboardConfirmed(Long tenantId, Long candidateId, double defaultRewardAmount) {
        LambdaQueryWrapper<Referral> w = new LambdaQueryWrapper<>();
        w.eq(Referral::getTenantId, tenantId).eq(Referral::getCandidateId, candidateId).last("LIMIT 1");
        Referral referral = referralMapper.selectOne(w);
        if (referral == null) {
            return;
        }
        referral.setStatus("HIRED");
        referral.setRewardAmount(java.math.BigDecimal.valueOf(defaultRewardAmount));
        referral.setRewardStatus("PENDING");
        referralMapper.updateById(referral);
    }

    private ReferralShareLink requireValidLink(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BizException("Invalid referral link");
        }
        LambdaQueryWrapper<ReferralShareLink> w = new LambdaQueryWrapper<>();
        w.eq(ReferralShareLink::getToken, token);
        ReferralShareLink link = shareLinkMapper.selectOne(w);
        if (link == null) {
            throw new BizException("Referral link not found");
        }
        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BizException("Referral link expired");
        }
        return link;
    }
}
