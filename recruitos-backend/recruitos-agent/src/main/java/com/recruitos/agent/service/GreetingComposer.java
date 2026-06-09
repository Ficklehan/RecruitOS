package com.recruitos.agent.service;

import com.recruitos.agent.mapper.CommunicationProfileReadMapper;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.tenant.TenantContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class GreetingComposer {

    private static final Pattern UNSAFE_PATTERN = Pattern.compile(
            "保证\\d+万|100%录取|限男|限女|35周岁|歧视",
            Pattern.CASE_INSENSITIVE);

    @Resource
    private CommunicationProfileReadMapper profileReadMapper;

    public String composeGreeting(Long jobId, String jobTitle, String candidateName, String kind) {
        Map<String, Object> profile = resolveProfile(jobId);
        String persona = str(profile, "persona", "专业招聘顾问");
        String logic = str(profile, "communicationLogic", "先确认意向，再介绍岗位");
        String company = str(profile, "companyBackground", "");

        String name = StringUtils.hasText(candidateName) ? candidateName : "您好";
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("，您好！");
        if ("RECHAT".equals(kind)) {
            sb.append("之前跟您聊过").append(jobTitle).append("的机会，想再确认一下您的意向。");
        } else {
            sb.append("我是负责").append(jobTitle).append("的招聘同事。");
            if (StringUtils.hasText(company) && company.length() < 80) {
                sb.append(company).append("。");
            }
            sb.append(logic.contains("简历") ? logic : logic + "，方便的话希望能进一步了解您的经历。");
        }
        sb.append("（").append(persona.length() > 20 ? persona.substring(0, 20) : persona).append("）");

        String message = sb.toString();
        validateSafety(message, str(profile, "guardrails", ""));
        return message;
    }

    public Map<String, Object> resolveProfile(Long jobId) {
        Long tenantId = TenantContext.getTenantId();
        Map<String, Object> merged = new LinkedHashMap<>();
        Map<String, Object> tenant = profileReadMapper.selectTenantDefault(tenantId);
        if (tenant != null) {
            merged.putAll(tenant);
        }
        if (jobId != null) {
            Map<String, Object> job = profileReadMapper.selectJobOverride(tenantId, jobId);
            if (job != null) {
                for (Map.Entry<String, Object> e : job.entrySet()) {
                    if (e.getValue() != null && StringUtils.hasText(e.getValue().toString())) {
                        merged.put(e.getKey(), e.getValue());
                    }
                }
            }
        }
        if (merged.isEmpty()) {
            merged.put("persona", "专业、简洁、尊重候选人的招聘顾问");
            merged.put("communicationLogic", "先确认意向，再介绍岗位亮点，最后索要简历");
            merged.put("guardrails", "不承诺薪资；不歧视；不过度施压");
        }
        return merged;
    }

    private void validateSafety(String message, String guardrails) {
        if (UNSAFE_PATTERN.matcher(message).find()) {
            throw new BizException("招呼话术未通过安全审查");
        }
        if (StringUtils.hasText(guardrails) && guardrails.contains("不承诺薪资")
                && message.matches(".*\\d+\\s*万.*")) {
            throw new BizException("招呼话术未通过安全审查：疑似承诺薪资");
        }
    }

    private String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v != null && StringUtils.hasText(v.toString()) ? v.toString() : def;
    }
}
