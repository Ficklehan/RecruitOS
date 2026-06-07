package com.recruitos.candidate.dto;

import java.io.Serializable;

/**
 * DTO for screening operation (pass/reject/reserve)
 */
public class ScreeningDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Screening status: PASSED/REJECTED/RESERVE */
    private String screeningStatus;

    /** Screener comment */
    private String screenerComment;

    // Getters and Setters

    public String getScreeningStatus() {
        return screeningStatus;
    }

    public void setScreeningStatus(String screeningStatus) {
        this.screeningStatus = screeningStatus;
    }

    public String getScreenerComment() {
        return screenerComment;
    }

    public void setScreenerComment(String screenerComment) {
        this.screenerComment = screenerComment;
    }
}
