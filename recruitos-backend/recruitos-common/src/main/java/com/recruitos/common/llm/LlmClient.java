package com.recruitos.common.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 调用 recruitos-llm 服务的 HTTP 客户端。
 */
@Component
@EnableConfigurationProperties(LlmClientProperties.class)
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    private final LlmClientProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public LlmClient(LlmClientProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getTimeoutMs());
        factory.setReadTimeout(properties.getTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
    }

    public String chat(LlmChatRequest request) {
        if (request == null || !properties.isEnabled()) {
            return null;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String url = properties.getBaseUrl().replaceAll("/$", "") + "/api/llm/chat";
            String body = restTemplate.postForObject(url, new HttpEntity<>(request, headers), String.class);
            if (body == null) {
                return null;
            }
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.has("data") ? root.get("data") : root;
            if (data.has("content")) {
                return data.get("content").asText();
            }
            if (data.has("answer")) {
                return data.get("answer").asText();
            }
            return body;
        } catch (Exception e) {
            log.warn("LLM chat failed: {}", e.getMessage());
            return null;
        }
    }
}
