package com.recruitos.common.match;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 岗位 JD 标签与候选人画像的统一匹配分（0–100）。
 * candidate / agent 筛选 / 决策面板共用。
 */
public final class TagMatchScoreCalculator {

    private TagMatchScoreCalculator() {
    }

    public static BigDecimal calculate(String jobTagsJson, CandidateSkillProfile candidate, ObjectMapper objectMapper) {
        if (candidate == null) {
            return BigDecimal.ZERO;
        }
        String skillBlob = candidate.toSkillBlob();
        JdTagMatcher.JdTagMatchResult jd = JdTagMatcher.match(jobTagsJson, skillBlob, objectMapper);

        double skillPart;
        if (jd.hasTags()) {
            skillPart = jd.getSkillMatchScore();
            if (jd.getRequiredTotalCount() > 0 && jd.getRequiredHitCount() < jd.getRequiredTotalCount()) {
                double missRatio = 1.0 - (double) jd.getRequiredHitCount() / jd.getRequiredTotalCount();
                skillPart = Math.max(20, skillPart - missRatio * 25);
            }
        } else if (hasText(skillBlob)) {
            skillPart = 55;
        } else {
            skillPart = 35;
        }

        double expPart = candidate.getWorkYears() != null && candidate.getWorkYears() >= 3 ? 72 : 45;
        double eduPart = 50;
        if (hasText(candidate.getEducation())) {
            String edu = candidate.getEducation();
            if (edu.contains("博士") || edu.contains("PhD")) {
                eduPart = 80;
            } else if (edu.contains("硕士") || edu.contains("Master")) {
                eduPart = 72;
            } else if (edu.contains("本科") || edu.contains("Bachelor")) {
                eduPart = 62;
            } else {
                eduPart = 55;
            }
        }

        double completeness = candidate.isHasParsedResume() ? 78 : (hasText(skillBlob) ? 60 : 35);
        double total = skillPart * 0.55 + expPart * 0.20 + eduPart * 0.15 + completeness * 0.10;
        total = Math.max(0, Math.min(100, total));
        return BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    public static class CandidateSkillProfile {
        private String tags;
        private String currentTitle;
        private String currentCompany;
        private Integer workYears;
        private String education;
        private boolean hasParsedResume;

        public String toSkillBlob() {
            StringBuilder sb = new StringBuilder();
            if (hasText(tags)) {
                sb.append(tags).append(',');
            }
            if (hasText(currentTitle)) {
                sb.append(currentTitle).append(',');
            }
            if (hasText(currentCompany)) {
                sb.append(currentCompany).append(',');
            }
            return sb.toString();
        }

        public String getTags() {
            return tags;
        }

        public void setTags(String tags) {
            this.tags = tags;
        }

        public String getCurrentTitle() {
            return currentTitle;
        }

        public void setCurrentTitle(String currentTitle) {
            this.currentTitle = currentTitle;
        }

        public String getCurrentCompany() {
            return currentCompany;
        }

        public void setCurrentCompany(String currentCompany) {
            this.currentCompany = currentCompany;
        }

        public Integer getWorkYears() {
            return workYears;
        }

        public void setWorkYears(Integer workYears) {
            this.workYears = workYears;
        }

        public String getEducation() {
            return education;
        }

        public void setEducation(String education) {
            this.education = education;
        }

        public boolean isHasParsedResume() {
            return hasParsedResume;
        }

        public void setHasParsedResume(boolean hasParsedResume) {
            this.hasParsedResume = hasParsedResume;
        }
    }
}
