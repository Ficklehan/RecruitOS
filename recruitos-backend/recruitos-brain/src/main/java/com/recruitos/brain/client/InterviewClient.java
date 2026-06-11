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
public class InterviewClient {
    private static final Logger log = LoggerFactory.getLogger(InterviewClient.class);
    @Resource private ServiceClientProperties props;
    @Resource private ObjectMapper objectMapper;
    private RestTemplate rest;

    @PostConstruct void init() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(props.getTimeoutMs()); f.setReadTimeout(props.getTimeoutMs());
        rest = new RestTemplate(f);
    }

    /** Get interviews by candidate */
    public List<Map<String, Object>> getByCandidate(Long candidateId) {
        return getList("/api/interview/list?candidateId=" + candidateId);
    }

    /** Get interview detail */
    public Map<String, Object> getInterview(Long interviewId) {
        return get("/api/interview/" + interviewId);
    }

    /** Get evaluations for an interview */
    public List<Map<String, Object>> getEvaluations(Long interviewId) {
        return getList("/api/interview/evaluation/list?interviewId=" + interviewId);
    }

    /** Get all evaluations by an interviewer */
    public List<Map<String, Object>> getEvaluationsByInterviewer(Long interviewerId) {
        return getList("/api/interview/evaluation/list?interviewerId=" + interviewerId);
    }

    private Map<String, Object> get(String path) {
        try {
            JsonNode node = rest.getForObject(props.getInterviewUrl() + path, JsonNode.class);
            if (node != null && node.has("data")) return objectMapper.convertValue(node.get("data"), Map.class);
        } catch (Exception e) { log.warn("InterviewClient GET {} failed: {}", path, e.getMessage()); }
        return null;
    }

    private List<Map<String, Object>> getList(String path) {
        Map<String, Object> data = get(path);
        if (data == null) return Collections.emptyList();
        Object records = data.get("records");
        if (records instanceof List) return (List<Map<String, Object>>) records;
        Object list = data.get("list");
        if (list instanceof List) return (List<Map<String, Object>>) list;
        return Collections.emptyList();
    }
}
