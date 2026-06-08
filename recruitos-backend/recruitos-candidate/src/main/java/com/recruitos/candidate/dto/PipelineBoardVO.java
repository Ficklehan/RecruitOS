package com.recruitos.candidate.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PipelineBoardVO implements Serializable {

    private Long jobId;
    private String jobTitle;
    private List<StageColumn> columns = new ArrayList<>();

    public static class StageColumn implements Serializable {
        private String stage;
        private String label;
        private List<CandidateJobVO> items = new ArrayList<>();

        public String getStage() { return stage; }
        public void setStage(String stage) { this.stage = stage; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public List<CandidateJobVO> getItems() { return items; }
        public void setItems(List<CandidateJobVO> items) { this.items = items; }
    }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public List<StageColumn> getColumns() { return columns; }
    public void setColumns(List<StageColumn> columns) { this.columns = columns; }
}
