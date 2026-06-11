package com.recruitos.brain.domain;

import java.io.Serializable; import java.util.List;

/** 触点6：AI Offer谈判策略 */
public class OfferStrategy implements Serializable {
    private Long candidateId; private String candidateName; private Long jobId;
    private String jobTitle; private String jobLevel;
    private SuggestedRange suggestedRange; private List<CompComponent> components;
    private List<String> negotiationTips; private List<RiskItem> risks;
    private String strategySummary; private Double confidence;

    public static class SuggestedRange implements Serializable {
        private int min; private int mid; private int max; private String currency;
        public int getMin() { return min; } public void setMin(int m) { min = m; }
        public int getMid() { return mid; } public void setMid(int m) { mid = m; }
        public int getMax() { return max; } public void setMax(int m) { max = m; }
        public String getCurrency() { return currency; } public void setCurrency(String c) { currency = c; }
    }
    public static class CompComponent implements Serializable {
        private String type; private int amount; private String note;
        public String getType() { return type; } public void setType(String t) { type = t; }
        public int getAmount() { return amount; } public void setAmount(int a) { amount = a; }
        public String getNote() { return note; } public void setNote(String n) { note = n; }
    }
    public static class RiskItem implements Serializable {
        private String risk; private String severity;
        public String getRisk() { return risk; } public void setRisk(String r) { risk = r; }
        public String getSeverity() { return severity; } public void setSeverity(String s) { severity = s; }
    }

    public Long getCandidateId() { return candidateId; } public void setCandidateId(Long c) { candidateId = c; }
    public String getCandidateName() { return candidateName; } public void setCandidateName(String n) { candidateName = n; }
    public Long getJobId() { return jobId; } public void setJobId(Long j) { jobId = j; }
    public String getJobTitle() { return jobTitle; } public void setJobTitle(String t) { jobTitle = t; }
    public String getJobLevel() { return jobLevel; } public void setJobLevel(String l) { jobLevel = l; }
    public SuggestedRange getSuggestedRange() { return suggestedRange; } public void setSuggestedRange(SuggestedRange r) { suggestedRange = r; }
    public List<CompComponent> getComponents() { return components; } public void setComponents(List<CompComponent> c) { components = c; }
    public List<String> getNegotiationTips() { return negotiationTips; } public void setNegotiationTips(List<String> t) { negotiationTips = t; }
    public List<RiskItem> getRisks() { return risks; } public void setRisks(List<RiskItem> r) { risks = r; }
    public String getStrategySummary() { return strategySummary; } public void setStrategySummary(String s) { strategySummary = s; }
    public Double getConfidence() { return confidence; } public void setConfidence(Double c) { confidence = c; }
}
