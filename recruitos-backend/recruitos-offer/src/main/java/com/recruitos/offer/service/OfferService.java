package com.recruitos.offer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.offer.dto.*;
import com.recruitos.offer.entity.Offer;
import com.recruitos.offer.mapper.OfferMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Offer service - core business logic for offer management
 *
 * Status flow:
 * DRAFT -> PENDING -> APPROVED -> SENT -> ACCEPTED
 *                  -> REJECTED
 *                                    -> EXPIRED
 */
@Service
public class OfferService {

    @Resource
    private OfferMapper offerMapper;

    /**
     * Create an offer with DRAFT status
     */
    @Transactional
    public OfferVO createOffer(OfferCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BizException("User not authenticated");
        }

        Offer offer = new Offer();
        offer.setTenantId(tenantId);
        offer.setCandidateId(dto.getCandidateId());
        offer.setCandidateName(dto.getCandidateName());
        offer.setJobId(dto.getJobId());
        offer.setJobTitle(dto.getJobTitle());
        offer.setDepartment(dto.getDepartment());
        if (dto.getSalary() != null) {
            offer.setSalary(new BigDecimal(dto.getSalary()));
        }
        offer.setRemark(dto.getRemark());
        offer.setStatus("DRAFT");
        offer.setCreatedBy(tenantId);

        offerMapper.insert(offer);

        return convertToVO(offer);
    }

    /**
     * Submit offer for approval - change status from DRAFT to PENDING
     */
    @Transactional
    public OfferVO submitApproval(Long id) {
        Long tenantId = TenantContext.getTenantId();

        Offer offer = offerMapper.selectById(id);
        if (offer == null) {
            throw new BizException("Offer not found");
        }
        if (!offer.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if (!"DRAFT".equals(offer.getStatus())) {
            throw new BizException("Only DRAFT offers can be submitted for approval");
        }

        offer.setStatus("PENDING");
        offerMapper.updateById(offer);

        return convertToVO(offer);
    }

    /**
     * Approve or reject an offer
     */
    @Transactional
    public OfferVO approve(Long id, OfferApprovalDTO dto) {
        Long tenantId = TenantContext.getTenantId();

        Offer offer = offerMapper.selectById(id);
        if (offer == null) {
            throw new BizException("Offer not found");
        }
        if (!offer.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if (!"PENDING".equals(offer.getStatus())) {
            throw new BizException("Only PENDING offers can be approved");
        }

        if ("APPROVED".equals(dto.getResult())) {
            offer.setStatus("APPROVED");
            offer.setApproverId(dto.getApproverId());
            offer.setApprovedAt(LocalDateTime.now());
        } else if ("REJECTED".equals(dto.getResult())) {
            offer.setStatus("REJECTED");
            offer.setApproverId(dto.getApproverId());
            offer.setApprovedAt(LocalDateTime.now());
        } else {
            throw new BizException("Invalid approval result: " + dto.getResult());
        }

        if (StringUtils.hasText(dto.getRemark())) {
            offer.setRemark(dto.getRemark());
        }

        offerMapper.updateById(offer);

        return convertToVO(offer);
    }

    /**
     * Send an approved offer - change status from APPROVED to SENT
     */
    @Transactional
    public OfferVO sendOffer(Long id) {
        Long tenantId = TenantContext.getTenantId();

        Offer offer = offerMapper.selectById(id);
        if (offer == null) {
            throw new BizException("Offer not found");
        }
        if (!offer.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if (!"APPROVED".equals(offer.getStatus())) {
            throw new BizException("Only APPROVED offers can be sent");
        }

        offer.setStatus("SENT");
        offer.setSentAt(LocalDateTime.now());
        offerMapper.updateById(offer);

        return convertToVO(offer);
    }

    /**
     * Accept an offer - change status from SENT to ACCEPTED
     */
    @Transactional
    public OfferVO acceptOffer(Long id) {
        Long tenantId = TenantContext.getTenantId();

        Offer offer = offerMapper.selectById(id);
        if (offer == null) {
            throw new BizException("Offer not found");
        }
        if (!offer.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if (!"SENT".equals(offer.getStatus())) {
            throw new BizException("Only SENT offers can be accepted");
        }

        offer.setStatus("ACCEPTED");
        offer.setAcceptedAt(LocalDateTime.now());
        offerMapper.updateById(offer);

        return convertToVO(offer);
    }

    /**
     * Reject an offer - change status from SENT to REJECTED
     */
    @Transactional
    public OfferVO rejectOffer(Long id) {
        Long tenantId = TenantContext.getTenantId();

        Offer offer = offerMapper.selectById(id);
        if (offer == null) {
            throw new BizException("Offer not found");
        }
        if (!offer.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if (!"SENT".equals(offer.getStatus())) {
            throw new BizException("Only SENT offers can be rejected");
        }

        offer.setStatus("REJECTED");
        offerMapper.updateById(offer);

        return convertToVO(offer);
    }

    /**
     * Get paginated offer list with filters
     */
    public PageResult<OfferVO> getOfferList(OfferQueryDTO query) {
        Long tenantId = TenantContext.getTenantId();

        Page<Offer> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<Offer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Offer::getTenantId, tenantId);

        if (query.getCandidateId() != null) {
            wrapper.eq(Offer::getCandidateId, query.getCandidateId());
        }
        if (query.getJobId() != null) {
            wrapper.eq(Offer::getJobId, query.getJobId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Offer::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getDepartment())) {
            wrapper.eq(Offer::getDepartment, query.getDepartment());
        }
        if (StringUtils.hasText(query.getCandidateName())) {
            wrapper.like(Offer::getCandidateName, query.getCandidateName());
        }

        wrapper.orderByDesc(Offer::getCreatedAt);

        Page<Offer> result = offerMapper.selectPage(page, wrapper);

        List<OfferVO> voList = new ArrayList<>();
        for (Offer offer : result.getRecords()) {
            voList.add(convertToVO(offer));
        }

        return new PageResult<>(result.getTotal(), voList, query.getPageNum(), query.getPageSize());
    }

    /**
     * Get offer detail by ID
     */
    public OfferVO getOfferDetail(Long id) {
        Long tenantId = TenantContext.getTenantId();

        Offer offer = offerMapper.selectById(id);
        if (offer == null) {
            throw new BizException("Offer not found");
        }
        if (!offer.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        return convertToVO(offer);
    }

    /**
     * Convert Offer entity to OfferVO
     */
    private OfferVO convertToVO(Offer offer) {
        OfferVO vo = new OfferVO();
        vo.setId(offer.getId());
        vo.setTenantId(offer.getTenantId());
        vo.setCandidateId(offer.getCandidateId());
        vo.setCandidateName(offer.getCandidateName());
        vo.setJobId(offer.getJobId());
        vo.setJobTitle(offer.getJobTitle());
        vo.setDepartment(offer.getDepartment());
        vo.setSalary(offer.getSalary());
        vo.setStatus(offer.getStatus());
        vo.setApproverId(offer.getApproverId());
        vo.setApprovedAt(offer.getApprovedAt());
        vo.setSentAt(offer.getSentAt());
        vo.setAcceptedAt(offer.getAcceptedAt());
        vo.setRemark(offer.getRemark());
        vo.setCreatedBy(offer.getCreatedBy());
        vo.setCreatedAt(offer.getCreatedAt());
        vo.setUpdatedAt(offer.getUpdatedAt());
        return vo;
    }
}
