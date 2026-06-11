package com.recruitos.agent.service;

import com.recruitos.agent.mapper.CommunicationProfileReadMapper;
import com.recruitos.agent.platform.PlatformCandidate;
import com.recruitos.common.communication.ScriptDecisionTree;
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
        return composeGreeting(jobId, jobTitle, candidateName, kind, null);
    }

    public String composeGreeting(Long jobId, String jobTitle, String candidateName, String kind,
                                  PlatformCandidate candidate) {
        Map<String, Object> profile = resolveProfile(jobId);
        String persona = str(profile, "persona", "专业招聘顾问");
        String companyBg = str(profile, "companyBackground", "");

        ScriptDecisionTree.GreetingCandidateContext ctx = new ScriptDecisionTree.GreetingCandidateContext();
        if (candidate != null) {
            ctx.setCompany(candidate.getCompany());
            ctx.setWorkYears(candidate.getWorkYears());
        }
        ScriptDecisionTree.Branch branch = ScriptDecisionTree.resolve(kind, ctx, profile);
        StringBuilder sb = new StringBuilder(ScriptDecisionTree.openingFor(branch, jobTitle, candidateName, profile));
        if (branch == ScriptDecisionTree.Branch.DEFAULT && StringUtils.hasText(companyBg) && companyBg.length() < 80) {
            sb.append(companyBg).append("。");
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
