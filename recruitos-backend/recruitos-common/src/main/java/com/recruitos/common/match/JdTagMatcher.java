package com.recruitos.common.match;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * 岗位 JD 标签与候选人技能标签逐条对照
 */
public final class JdTagMatcher {

    private JdTagMatcher() {
    }

    public static JdTagMatchResult match(String jobTagsJson, String candidateTags, ObjectMapper objectMapper) {
        JdTagMatchResult result = new JdTagMatchResult();
        List<JobTagSpec> jobTags = parseJobTags(jobTagsJson, objectMapper);
        if (jobTags.isEmpty()) {
            return result;
        }

        String candidateBlob = normalizeBlob(candidateTags);
        List<Map<String, Object>> rows = new ArrayList<>();
        double weightSum = 0;
        double weightedScoreSum = 0;
        int hits = 0;
        int requiredHits = 0;
        int requiredTotal = 0;
        int preferredHits = 0;
        int preferredTotal = 0;

        int limit = Math.min(jobTags.size(), 10);
        for (int i = 0; i < limit; i++) {
            JobTagSpec spec = jobTags.get(i);
            double w = effectiveWeight(spec);
            boolean matched = matches(candidateBlob, spec.name);
            int score = matched ? (int) Math.round(70 + w * 25) : 20;
            if (matched) {
                hits++;
            }
            if (spec.isRequired()) {
                requiredTotal++;
                if (matched) {
                    requiredHits++;
                }
            } else {
                preferredTotal++;
                if (matched) {
                    preferredHits++;
                }
            }
            weightSum += w;
            weightedScoreSum += score * w;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", spec.name);
            row.put("score", Math.min(score, 100));
            row.put("weight", round2(w));
            row.put("correlation", round2(score / 100.0));
            row.put("trend", matched ? "up" : "down");
            row.put("matched", matched);
            row.put("requirementType", spec.requirementType);
            row.put("requirementLabel", requirementLabel(spec.requirementType));
            row.put("importance", spec.importance);
            rows.add(row);
        }

        result.setJdTagMatches(rows);
        result.setHitCount(hits);
        result.setTotalCount(limit);
        result.setRequiredHitCount(requiredHits);
        result.setRequiredTotalCount(requiredTotal);
        result.setPreferredHitCount(preferredHits);
        result.setPreferredTotalCount(preferredTotal);
        result.setSkillMatchScore(weightSum > 0
                ? (int) Math.round(weightedScoreSum / weightSum)
                : (hits * 100 / Math.max(limit, 1)));
        return result;
    }

    private static boolean matches(String candidateBlob, String jobTag) {
        if (candidateBlob.isEmpty() || jobTag == null || jobTag.trim().isEmpty()) {
            return false;
        }
        String jt = normalizeToken(jobTag);
        if (candidateBlob.contains(jt)) {
            return true;
        }
        for (String part : candidateBlob.split("[,，、\\s]+")) {
            if (part.isEmpty()) {
                continue;
            }
            if (part.contains(jt) || jt.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeBlob(String candidateTags) {
        if (candidateTags == null) {
            return "";
        }
        String raw = candidateTags.trim();
        if (raw.startsWith("[") || raw.startsWith("{")) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                if (raw.startsWith("[")) {
                    List<Object> list = mapper.readValue(raw, new TypeReference<List<Object>>() {});
                    StringBuilder sb = new StringBuilder();
                    for (Object item : list) {
                        if (item instanceof Map) {
                            Object tag = ((Map<?, ?>) item).get("tag");
                            if (tag == null) {
                                tag = ((Map<?, ?>) item).get("name");
                            }
                            if (tag != null) {
                                sb.append(normalizeToken(String.valueOf(tag))).append(',');
                            }
                        } else if (item != null) {
                            sb.append(normalizeToken(String.valueOf(item))).append(',');
                        }
                    }
                    return sb.toString();
                }
            } catch (Exception ignored) {
                /* use raw */
            }
        }
        return normalizeToken(raw);
    }

    private static List<JobTagSpec> parseJobTags(String jobTagsJson, ObjectMapper objectMapper) {
        if (jobTagsJson == null || jobTagsJson.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            ObjectMapper mapper = objectMapper != null ? objectMapper : new ObjectMapper();
            List<Map<String, Object>> list = mapper.readValue(jobTagsJson.trim(),
                    new TypeReference<List<Map<String, Object>>>() {});
            List<JobTagSpec> specs = new ArrayList<>();
            for (Map<String, Object> m : list) {
                Object tagObj = m.get("tag");
                if (tagObj == null) {
                    tagObj = m.get("name");
                }
                if (tagObj == null) {
                    continue;
                }
                JobTagSpec spec = new JobTagSpec();
                spec.name = String.valueOf(tagObj);
                spec.matchWeight = toDouble(m.get("matchWeight"), m.get("match_weight"));
                spec.decisionWeight = toDouble(m.get("decisionWeight"), m.get("decision_weight"));
                spec.requirementType = parseRequirementType(m);
                spec.importance = parseImportance(m);
                specs.add(spec);
            }
            specs.sort((a, b) -> {
                int typeCmp = Boolean.compare(b.isRequired(), a.isRequired());
                if (typeCmp != 0) {
                    return typeCmp;
                }
                return Double.compare(effectiveWeight(b), effectiveWeight(a));
            });
            return specs;
        } catch (Exception e) {
            List<JobTagSpec> specs = new ArrayList<>();
            for (String part : jobTagsJson.split("[,，、\\s]+")) {
                if (!part.trim().isEmpty()) {
                    JobTagSpec spec = new JobTagSpec();
                    spec.name = part.trim();
                    spec.matchWeight = 1.0;
                    spec.decisionWeight = 1.0;
                    spec.requirementType = "REQUIRED";
                    spec.importance = "MEDIUM";
                    specs.add(spec);
                }
            }
            return specs;
        }
    }

    private static double toDouble(Object primary, Object fallback) {
        Object v = primary != null ? primary : fallback;
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        if (v != null) {
            try {
                return Double.parseDouble(String.valueOf(v));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static String normalizeToken(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static double effectiveWeight(JobTagSpec spec) {
        double w = spec.decisionWeight > 0 ? spec.decisionWeight : spec.matchWeight;
        if (w <= 0) {
            w = 0.6;
        }
        if ("REQUIRED".equalsIgnoreCase(spec.requirementType)) {
            w *= 1.15;
        } else if ("PREFERRED".equalsIgnoreCase(spec.requirementType)) {
            w *= 0.85;
        }
        if ("HIGH".equalsIgnoreCase(spec.importance)) {
            w *= 1.1;
        } else if ("LOW".equalsIgnoreCase(spec.importance)) {
            w *= 0.85;
        }
        return Math.min(w, 1.0);
    }

    private static String parseRequirementType(Map<String, Object> m) {
        Object v = m.get("requirementType");
        if (v == null) {
            v = m.get("requirement_type");
        }
        if (v != null) {
            String s = String.valueOf(v).trim().toUpperCase(Locale.ROOT);
            if ("REQUIRED".equals(s) || "PREFERRED".equals(s)) {
                return s;
            }
        }
        double decision = toDouble(m.get("decisionWeight"), m.get("decision_weight"));
        return decision >= 0.68 ? "REQUIRED" : "PREFERRED";
    }

    private static String parseImportance(Map<String, Object> m) {
        Object v = m.get("importance");
        if (v != null) {
            String s = String.valueOf(v).trim().toUpperCase(Locale.ROOT);
            if ("HIGH".equals(s) || "MEDIUM".equals(s) || "LOW".equals(s)) {
                return s;
            }
        }
        double match = toDouble(m.get("matchWeight"), m.get("match_weight"));
        if (match >= 0.72) {
            return "HIGH";
        }
        if (match >= 0.48) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private static String requirementLabel(String type) {
        if ("REQUIRED".equalsIgnoreCase(type)) {
            return "必备";
        }
        if ("PREFERRED".equalsIgnoreCase(type)) {
            return "加分";
        }
        return "要求";
    }

    private static class JobTagSpec {
        String name;
        double matchWeight;
        double decisionWeight;
        String requirementType = "PREFERRED";
        String importance = "MEDIUM";

        boolean isRequired() {
            return "REQUIRED".equalsIgnoreCase(requirementType);
        }
    }

    public static class JdTagMatchResult {
        private List<Map<String, Object>> jdTagMatches = Collections.emptyList();
        private int hitCount;
        private int totalCount;
        private int requiredHitCount;
        private int requiredTotalCount;
        private int preferredHitCount;
        private int preferredTotalCount;
        private int skillMatchScore;

        public List<Map<String, Object>> getJdTagMatches() {
            return jdTagMatches;
        }

        public void setJdTagMatches(List<Map<String, Object>> jdTagMatches) {
            this.jdTagMatches = jdTagMatches;
        }

        public int getHitCount() {
            return hitCount;
        }

        public void setHitCount(int hitCount) {
            this.hitCount = hitCount;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }

        public int getRequiredHitCount() {
            return requiredHitCount;
        }

        public void setRequiredHitCount(int requiredHitCount) {
            this.requiredHitCount = requiredHitCount;
        }

        public int getRequiredTotalCount() {
            return requiredTotalCount;
        }

        public void setRequiredTotalCount(int requiredTotalCount) {
            this.requiredTotalCount = requiredTotalCount;
        }

        public int getPreferredHitCount() {
            return preferredHitCount;
        }

        public void setPreferredHitCount(int preferredHitCount) {
            this.preferredHitCount = preferredHitCount;
        }

        public int getPreferredTotalCount() {
            return preferredTotalCount;
        }

        public void setPreferredTotalCount(int preferredTotalCount) {
            this.preferredTotalCount = preferredTotalCount;
        }

        public int getSkillMatchScore() {
            return skillMatchScore;
        }

        public void setSkillMatchScore(int skillMatchScore) {
            this.skillMatchScore = skillMatchScore;
        }

        public boolean hasTags() {
            return totalCount > 0;
        }
    }
}
