package com.recruitos.job.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * DTO for creating a job position
 */
public class JobCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Associated recruitment demand ID */
    @NotNull(message = "Demand ID is required")
    private Long demandId;

    /** Job title */
    @NotBlank(message = "Job title is required")
    private String title;

    /** Job description text */
    private String jdText;

    /** Total head count for this position */
    @NotNull(message = "Head count is required")
    @Min(value = 1, message = "Head count must be at least 1")
    private Integer headCount;

    // Getters and Setters

    public Long getDemandId() {
        return demandId;
    }

    public void setDemandId(Long demandId) {
        this.demandId = demandId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getJdText() {
        return jdText;
    }

    public void setJdText(String jdText) {
        this.jdText = jdText;
    }

    public Integer getHeadCount() {
        return headCount;
    }

    public void setHeadCount(Integer headCount) {
        this.headCount = headCount;
    }
}
