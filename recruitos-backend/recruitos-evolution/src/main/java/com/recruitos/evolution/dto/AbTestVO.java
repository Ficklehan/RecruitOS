package com.recruitos.evolution.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Value Object for A/B test response
 */
public class AbTestVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private String testName;
    private String testType;
    private Long jobId;
    private String jobTitle;
    private String variantA;
    private String variantB;
    private String status;
    private String winnerVariant;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer sampleSizeA;
    private Integer sampleSizeB;
    private Double conversionRateA;
    private Double conversionRateB;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
