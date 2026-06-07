package com.recruitos.evolution.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

import java.time.LocalDate;

/**
 * A/B test entity - manages weight optimization experiments
 */
@TableName("ab_test")
public class AbTest extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Test name */
    private String testName;

    /** Test type: WEIGHT/MATCH/SEARCH */
    private String testType;

    /** Job ID */
    private Long jobId;

    /** Job title */
    private String jobTitle;

    /** Variant A configuration (JSON) */
    private String variantA;

    /** Variant B configuration (JSON) */
    private String variantB;

    /** Status: DRAFT/RUNNING/COMPLETED/CANCELLED */
    private String status;

    /** Winner variant: A/B/NONE */
    private String winnerVariant;

    /** Test start date */
    private LocalDate startDate;

    /** Test end date */
    private LocalDate endDate;

    /** Sample size for variant A */
    private Integer sampleSizeA;

    /** Sample size for variant B */
    private Integer sampleSizeB;

    /** Conversion rate for variant A */
    private Double conversionRateA;

    /** Conversion rate for variant B */
    private Double conversionRateB;

    /** Created by user ID */
    private Long createdBy;

    // Getters and Setters

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = testType;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getVariantA() {
        return variantA;
    }

    public void setVariantA(String variantA) {
        this.variantA = variantA;
    }

    public String getVariantB() {
        return variantB;
    }

    public void setVariantB(String variantB) {
        this.variantB = variantB;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getWinnerVariant() {
        return winnerVariant;
    }

    public void setWinnerVariant(String winnerVariant) {
        this.winnerVariant = winnerVariant;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getSampleSizeA() {
        return sampleSizeA;
    }

    public void setSampleSizeA(Integer sampleSizeA) {
        this.sampleSizeA = sampleSizeA;
    }

    public Integer getSampleSizeB() {
        return sampleSizeB;
    }

    public void setSampleSizeB(Integer sampleSizeB) {
        this.sampleSizeB = sampleSizeB;
    }

    public Double getConversionRateA() {
        return conversionRateA;
    }

    public void setConversionRateA(Double conversionRateA) {
        this.conversionRateA = conversionRateA;
    }

    public Double getConversionRateB() {
        return conversionRateB;
    }

    public void setConversionRateB(Double conversionRateB) {
        this.conversionRateB = conversionRateB;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}
