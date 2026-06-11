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
public class JobClient {
    private static final Logger log = LoggerFactory.getLogger(JobClient.class);
    @Resource private ServiceClientProperties props;
    @Resource private ObjectMapper objectMapper;
    private RestTemplate rest;

    @PostConstruct void init() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(props.getTimeoutMs()); f.setReadTimeout(props.getTimeoutMs());
        rest = new RestTemplate(f);
    }

    /** Get job detail including tags */
    public Map<String, Object> getJob(Long jobId) {
        try {
            JsonNode node = rest.getForObject(props.getJobUrl() + "/api/job/" + jobId, JsonNode.class);
            if (node != null && node.has("data")) return objectMapper.convertValue(node.get("data"), Map.class);
        } catch (Exception e) { log.warn("JobClient get failed: {}", e.getMessage()); }
        return null;
    }

    /** Get job tags for capability analysis */
    public Map<String, Object> getTags(Long jobId) {
        Map<String, Object> job = getJob(jobId);
        if (job == null) return null;
        Object tags = job.get("tags");
        if (tags instanceof Map) return (Map<String, Object>) tags;
        return null;
    }

    /** Get all active jobs for a tenant (for talent density) */
    public List<Map<String, Object>> getActiveJobs() {
        try {
            JsonNode node = rest.getForObject(props.getJobUrl() + "/api/job/list?status=ACTIVE", JsonNode.class);
            if (node != null && node.has("data")) {
                Map<String, Object> data = objectMapper.convertValue(node.get("data"), Map.class);
                Object records = data.get("records");
                if (records instanceof List) return (List<Map<String, Object>>) records;
            }
        } catch (Exception e) { log.warn("JobClient list failed: {}", e.getMessage()); }
        return Collections.emptyList();
    }
}
