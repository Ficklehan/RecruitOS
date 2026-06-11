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
public class OfferClient {
    private static final Logger log = LoggerFactory.getLogger(OfferClient.class);
    @Resource private ServiceClientProperties props;
    @Resource private ObjectMapper objectMapper;
    private RestTemplate rest;

    @PostConstruct void init() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(props.getTimeoutMs()); f.setReadTimeout(props.getTimeoutMs());
        rest = new RestTemplate(f);
    }

    /** Get offer history for a candidate (for intent label) */
    public List<Map<String, Object>> getOffersByCandidate(Long candidateId) {
        try {
            JsonNode node = rest.getForObject(props.getOfferUrl() + "/api/offer/list?candidateId=" + candidateId, JsonNode.class);
            if (node != null && node.has("data")) {
                Map<String, Object> data = objectMapper.convertValue(node.get("data"), Map.class);
                Object records = data.get("records");
                if (records instanceof List) return (List<Map<String, Object>>) records;
            }
        } catch (Exception e) { log.warn("OfferClient offers failed: {}", e.getMessage()); }
        return Collections.emptyList();
    }

    /** Get offer by candidate and job to check status */
    public Map<String, Object> getOfferByCandidateAndJob(Long candidateId, Long jobId) {
        List<Map<String, Object>> offers = getOffersByCandidate(candidateId);
        for (Map<String, Object> o : offers) {
            Object jid = o.get("jobId");
            if (jid != null && ((Number) jid).longValue() == jobId) return o;
        }
        return null;
    }

    /** Check if candidate has accepted an offer */
    public boolean hasAcceptedOffer(Long candidateId, Long jobId) {
        Map<String, Object> offer = getOfferByCandidateAndJob(candidateId, jobId);
        if (offer == null) return false;
        Object status = offer.get("status");
        return "ACCEPTED".equals(status);
    }
}
