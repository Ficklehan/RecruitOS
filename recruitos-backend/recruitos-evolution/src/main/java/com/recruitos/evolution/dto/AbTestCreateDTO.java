package com.recruitos.evolution.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO for creating an A/B test
 */
public class AbTestCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Test name is required")
    private String testName;

    @NotBlank(message = "Test type is required")
    private String testType;

    @NotNull(message = "Job ID is required")
    private Long jobId;

    private String jobTitle;

    @NotBlank(message = "Variant A config is required")
    private String variantA;

    @NotBlank(message = "Variant B config is required")
    private String variantB;

    private LocalDate startDate;

    private LocalDate endDate;

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
}
