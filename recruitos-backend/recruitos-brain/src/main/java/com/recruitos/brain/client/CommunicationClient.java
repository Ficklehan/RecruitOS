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
public class CommunicationClient {
    private static final Logger log = LoggerFactory.getLogger(CommunicationClient.class);
    @Resource private ServiceClientProperties props;
    @Resource private ObjectMapper objectMapper;
    private RestTemplate rest;

    @PostConstruct void init() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(props.getTimeoutMs()); f.setReadTimeout(props.getTimeoutMs());
        rest = new RestTemplate(f);
    }

    /** Get recent messages between system and candidate */
    public List<Map<String, Object>> getRecentMessages(Long candidateId, int limit) {
        try {
            JsonNode node = rest.getForObject(
                props.getCommunicationUrl() + "/api/communication/conversation?candidateId=" + candidateId + "&limit=" + limit,
                JsonNode.class);
            if (node != null && node.has("data")) {
                Map<String, Object> data = objectMapper.convertValue(node.get("data"), Map.class);
                Object records = data.get("records");
                if (records instanceof List) return (List<Map<String, Object>>) records;
            }
        } catch (Exception e) { log.warn("CommunicationClient messages failed: {}", e.getMessage()); }
        return Collections.emptyList();
    }

    /** Calculate average reply interval in hours from message timestamps */
    public double calculateReplySpeed(List<Map<String, Object>> messages) {
        if (messages == null || messages.size() < 2) return 0.5; // default
        List<Long> timestamps = new ArrayList<>();
        for (Map<String, Object> msg : messages) {
            Object ts = msg.get("createdAt");
            if (ts instanceof String) {
                try { timestamps.add(java.time.LocalDateTime.parse((String) ts, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()); }
                catch (Exception e) { /* skip */ }
            } else if (ts instanceof Number) {
                timestamps.add(((Number) ts).longValue());
            }
        }
        if (timestamps.size() < 2) return 0.5;
        Collections.sort(timestamps);
        long totalGap = 0;
        for (int i = 1; i < timestamps.size(); i++) totalGap += timestamps.get(i) - timestamps.get(i-1);
        double avgHours = totalGap / (double)(timestamps.size() - 1) / 3600000.0;
        // normalize: < 4h = fast (1.0), > 48h = slow (0.0)
        return Math.max(0, Math.min(1, 1 - (avgHours - 4) / 44));
    }
}
