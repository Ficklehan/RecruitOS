package com.recruitos.candidate.dto;

import java.io.Serializable;

public class PipelineAdvanceDTO implements Serializable {

    private String toStage;
    private String toSubStage;
    private String reasonCode;
    private String comment;
    private Boolean archivedToPool;

    public String getToStage() { return toStage; }
    public void setToStage(String toStage) { this.toStage = toStage; }
    public String getToSubStage() { return toSubStage; }
    public void setToSubStage(String toSubStage) { this.toSubStage = toSubStage; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Boolean getArchivedToPool() { return archivedToPool; }
    public void setArchivedToPool(Boolean archivedToPool) { this.archivedToPool = archivedToPool; }
}
