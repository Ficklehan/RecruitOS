package com.recruitos.evolution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruitos.common.auth.CurrentUser;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.evolution.dto.AbTestCreateDTO;
import com.recruitos.evolution.dto.AbTestQueryDTO;
import com.recruitos.evolution.dto.AbTestVO;
import com.recruitos.evolution.entity.AbTest;
import com.recruitos.evolution.mapper.AbTestMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A/B test service - manages weight optimization experiments
 */
@Service
public class AbTestService {

    @Resource
    private AbTestMapper abTestMapper;

    /**
     * Create a new A/B test
     */
    @Transactional
    public AbTestVO createTest(AbTestCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = CurrentUser.getCurrentUserId();
        if (tenantId == null || userId == null) {
            throw new BizException("User not authenticated");
        }

        AbTest test = new AbTest();
        test.setTenantId(tenantId);
        test.setTestName(dto.getTestName());
        test.setTestType(dto.getTestType());
        test.setJobId(dto.getJobId());
        test.setJobTitle(dto.getJobTitle());
        test.setVariantA(dto.getVariantA());
        test.setVariantB(dto.getVariantB());
        test.setStatus("DRAFT");
        test.setWinnerVariant("NONE");
        test.setStartDate(dto.getStartDate());
        test.setEndDate(dto.getEndDate());
        test.setSampleSizeA(0);
        test.setSampleSizeB(0);
        test.setConversionRateA(0.0);
        test.setConversionRateB(0.0);
        test.setCreatedBy(userId);

        abTestMapper.insert(test);

        return convertToVO(test);
    }

    /**
     * Start an A/B test
     */
    @Transactional
    public AbTestVO startTest(Long id) {
        Long tenantId = TenantContext.getTenantId();

        AbTest test = abTestMapper.selectById(id);
        if (test == null) {
            throw new BizException("A/B test not found");
        }
        if (!test.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if (!"DRAFT".equals(test.getStatus())) {
            throw new BizException("Only DRAFT tests can be started");
        }

        test.setStatus("RUNNING");
        if (test.getStartDate() == null) {
            test.setStartDate(LocalDate.now());
        }
        abTestMapper.updateById(test);

        return convertToVO(test);
    }

    /**
     * Stop an A/B test and declare winner
     */
    @Transactional
    public AbTestVO stopTest(Long id, String winnerVariant) {
        Long tenantId = TenantContext.getTenantId();

        AbTest test = abTestMapper.selectById(id);
        if (test == null) {
            throw new BizException("A/B test not found");
        }
        if (!test.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if (!"RUNNING".equals(test.getStatus())) {
            throw new BizException("Only RUNNING tests can be stopped");
        }

        test.setStatus("COMPLETED");
        test.setWinnerVariant(winnerVariant);
        test.setEndDate(LocalDate.now());
        abTestMapper.updateById(test);

        return convertToVO(test);
    }

    /**
     * Get paginated list of A/B tests with filters
     */
    public PageResult<AbTestVO> getTestList(AbTestQueryDTO query) {
        Long tenantId = TenantContext.getTenantId();

        Page<AbTest> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<AbTest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AbTest::getTenantId, tenantId);

        if (StringUtils.hasText(query.getTestName())) {
            wrapper.like(AbTest::getTestName, query.getTestName());
        }
        if (StringUtils.hasText(query.getTestType())) {
            wrapper.eq(AbTest::getTestType, query.getTestType());
        }
        if (query.getJobId() != null) {
            wrapper.eq(AbTest::getJobId, query.getJobId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(AbTest::getStatus, query.getStatus());
        }

        wrapper.orderByDesc(AbTest::getCreatedAt);

        Page<AbTest> result = abTestMapper.selectPage(page, wrapper);

        List<AbTestVO> voList = new ArrayList<>();
        for (AbTest test : result.getRecords()) {
            voList.add(convertToVO(test));
        }

        return new PageResult<>(result.getTotal(), voList, query.getPageNum(), query.getPageSize());
    }

    /**
     * Get A/B test detail by ID
     */
    public AbTestVO getTestDetail(Long id) {
        Long tenantId = TenantContext.getTenantId();

        AbTest test = abTestMapper.selectById(id);
        if (test == null) {
            throw new BizException("A/B test not found");
        }
        if (!test.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        return convertToVO(test);
    }

    /**
     * Convert entity to VO
     */
    private AbTestVO convertToVO(AbTest test) {
        AbTestVO vo = new AbTestVO();
        vo.setId(test.getId());
        vo.setTenantId(test.getTenantId());
        vo.setTestName(test.getTestName());
        vo.setTestType(test.getTestType());
        vo.setJobId(test.getJobId());
        vo.setJobTitle(test.getJobTitle());
        vo.setVariantA(test.getVariantA());
        vo.setVariantB(test.getVariantB());
        vo.setStatus(test.getStatus());
        vo.setWinnerVariant(test.getWinnerVariant());
        vo.setStartDate(test.getStartDate());
        vo.setEndDate(test.getEndDate());
        vo.setSampleSizeA(test.getSampleSizeA());
        vo.setSampleSizeB(test.getSampleSizeB());
        vo.setConversionRateA(test.getConversionRateA());
        vo.setConversionRateB(test.getConversionRateB());
        vo.setCreatedBy(test.getCreatedBy());
        vo.setCreatedAt(test.getCreatedAt());
        vo.setUpdatedAt(test.getUpdatedAt());
        return vo;
    }
}
