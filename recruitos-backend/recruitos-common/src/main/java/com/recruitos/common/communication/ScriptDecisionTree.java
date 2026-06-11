package com.recruitos.common.communication;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 招呼话术场景分支（MVP）：竞品 / 初级 / 复聊 / 标准。
 */
public final class ScriptDecisionTree {

    public enum Branch {
        COMPETITOR,
        JUNIOR,
        RECHAT,
        DEFAULT
    }

    private ScriptDecisionTree() {
    }

    public static Branch resolve(String kind, GreetingCandidateContext candidate, Map<String, Object> profile) {
        if ("RECHAT".equals(kind)) {
            return Branch.RECHAT;
        }
        if (candidate != null && isCompetitor(candidate.getCompany(), profile)) {
            return Branch.COMPETITOR;
        }
        if (candidate != null && candidate.getWorkYears() != null && candidate.getWorkYears() <= 2) {
            return Branch.JUNIOR;
        }
        return Branch.DEFAULT;
    }

    public static String openingFor(Branch branch, String jobTitle, String candidateName, Map<String, Object> profile) {
        String name = StringUtils.hasText(candidateName) ? candidateName : "您好";
        String title = StringUtils.hasText(jobTitle) ? jobTitle : "该岗位";
        switch (branch) {
            case COMPETITOR:
                return name + "，您好！注意到您目前在业内一线平台任职，我们在招「" + title + "」，岗位发展空间和团队技术氛围都不错，想了解一下您是否有兴趣聊聊？";
            case JUNIOR:
                return name + "，您好！我们在招「" + title + "」，团队对成长型同学很友好，欢迎了解机会、也欢迎投递简历进一步沟通。";
            case RECHAT:
                return name + "，您好！之前跟您聊过「" + title + "」的机会，想再确认一下您目前的意向。";
            default:
                String logic = str(profile, "communicationLogic", "先确认意向，再介绍岗位亮点");
                return name + "，您好！我是负责「" + title + "」的招聘同事，" + logic + "。";
        }
    }

    private static boolean isCompetitor(String company, Map<String, Object> profile) {
        if (!StringUtils.hasText(company) || profile == null) {
            return false;
        }
        String normalized = company.trim().toLowerCase(Locale.ROOT);
        for (String competitor : parseCompetitors(profile)) {
            if (normalized.contains(competitor.toLowerCase(Locale.ROOT))
                    || competitor.toLowerCase(Locale.ROOT).contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static List<String> parseCompetitors(Map<String, Object> profile) {
        Object raw = profile.get("competitorCompanies");
        if (raw == null) {
            raw = profile.get("proactiveTriggersJson");
        }
        if (raw == null) {
            return defaultCompetitors();
        }
        String text = raw.toString();
        if (text.trim().startsWith("{")) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> json = om.readValue(text, Map.class);
                Object list = json.get("competitors");
                if (list instanceof List) {
                    List<String> out = new ArrayList<>();
                    for (Object item : (List<?>) list) {
                        if (item != null && StringUtils.hasText(item.toString())) {
                            out.add(item.toString().trim());
                        }
                    }
                    if (!out.isEmpty()) {
                        return out;
                    }
                }
            } catch (Exception ignored) {
                /* fall through */
            }
        }
        if (text.contains("竞品:") || text.contains("竞品：")) {
            String part = text.contains("竞品:") ? text.substring(text.indexOf("竞品:") + 3) : text.substring(text.indexOf("竞品：") + 3);
            return splitList(part);
        }
        return splitList(text);
    }

    private static List<String> splitList(String text) {
        if (!StringUtils.hasText(text)) {
            return defaultCompetitors();
        }
        String[] parts = text.split("[,，、;；]");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            if (StringUtils.hasText(p)) {
                out.add(p.trim());
            }
        }
        return out.isEmpty() ? defaultCompetitors() : out;
    }

    private static List<String> defaultCompetitors() {
        return Collections.unmodifiableList(java.util.Arrays.asList(
                "字节跳动", "腾讯", "阿里巴巴", "美团", "京东", "百度", "华为"
        ));
    }

    private static String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v != null && StringUtils.hasText(v.toString()) ? v.toString() : def;
    }

    public static class GreetingCandidateContext {
        private String company;
        private Integer workYears;

        public String getCompany() {
            return company;
        }

        public void setCompany(String company) {
            this.company = company;
        }

        public Integer getWorkYears() {
            return workYears;
        }

        public void setWorkYears(Integer workYears) {
            this.workYears = workYears;
        }
    }
}
