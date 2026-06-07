package com.recruitos.job.dto;

import java.io.Serializable;

/**
 * DTO for updating a job position
 */
public class JobUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Job title */
    private String title;

    /** Job description text */
    private String jdText;

    /** Total head count for this position */
    private Integer headCount;

    // Getters and Setters

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
