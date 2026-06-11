package com.recruitos.brain.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;

@Component
public class CandidateClient {
    private static final Logger log = LoggerFactory.getLogger(CandidateClient.class);
    @Resource private ServiceClientProperties props;
    @Resource private ObjectMapper objectMapper;
    private RestTemplate rest;

    @PostConstruct
    void init() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(props.getTimeoutMs());
        f.setReadTimeout(props.getTimeoutMs());
        rest = new RestTemplate(f);
    }

    /** Get candidate basic info */
    public Map<String, Object> getCandidate(Long candidateId) {
        return get("/api/candidate/" + candidateId);
    }

    /** Get candidate's pipeline status */
    public Map<String, Object> getPipelineStatus(Long candidateId) {
        return get("/api/candidate/" + candidateId + "/pipeline");
    }

    /** Get candidate's parsed skills */
    public List<String> getSkills(Long candidateId) {
        Map<String, Object> data = get("/api/candidate/" + candidateId + "/skills");
        if (data == null) return Collections.emptyList();
        Object skills = data.get("skills");
        if (skills instanceof List) return (List<String>) skills;
        return Collections.emptyList();
    }

    /** Get candidates by job (for pipeline data) */
    public List<Map<String, Object>> listByJob(Long jobId) {
        Map<String, Object> data = get("/api/candidate/list?jobId=" + jobId);
        if (data == null) return Collections.emptyList();
        Object records = data.get("records");
        if (records instanceof List) return (List<Map<String, Object>>) records;
        return Collections.emptyList();
    }

    private Map<String, Object> get(String path) {
        try {
            JsonNode node = rest.getForObject(props.getCandidateUrl() + path, JsonNode.class);
            if (node != null && node.has("data")) {
                JsonNode dataNode = node.get("data");
                return objectMapper.convertValue(dataNode, Map.class);
            }
        } catch (Exception e) {
            log.warn("CandidateClient GET {} failed: {}", path, e.getMessage());
        }
        return null;
    }

    /**
     * 获取组织下所有成员的技能标签（用于人才密度评估）。
     */
    public List<Map<String, Object>> listSkillTagsByOrg(Long orgId) {
        try {
            JsonNode node = rest.getForObject(props.getCandidateUrl() + "/api/candidate/skill-tags/org/" + orgId, JsonNode.class);
            if (node != null && node.has("data")) {
                JsonNode dataNode = node.get("data");
                return objectMapper.convertValue(dataNode, List.class);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch skill tags for org {}", orgId, e);
        }
        return null;
    }
}
