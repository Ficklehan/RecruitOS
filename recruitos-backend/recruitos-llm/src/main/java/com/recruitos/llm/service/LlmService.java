package com.recruitos.llm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.recruitos.llm.dto.LlmChatDTO;
import com.recruitos.llm.dto.LlmChatVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.Map;

@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    @Value("${recruitos.llm.provider-base-url:https://api.xiaomimimo.com/v1}")
    private String providerBaseUrl;

    @Value("${recruitos.llm.api-key:}")
    private String apiKey;

    @Value("${recruitos.llm.auth-mode:api-key}")
    private String authMode;

    @Value("${recruitos.llm.model:mimo-v2.5}")
    private String model;

    @Value("${recruitos.llm.enabled:true}")
    private boolean enabled;

    @Value("${recruitos.llm.max-completion-tokens:2048}")
    private int maxCompletionTokens;

    @Value("${recruitos.llm.temperature:0.2}")
    private double temperature;

    @Resource
    private ObjectMapper objectMapper;

    private final RestTemplate restTemplate;

    public LlmService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(120000);
        this.restTemplate = new RestTemplate(factory);
    }

    public LlmChatVO chat(LlmChatDTO dto) {
        LlmChatVO vo = new LlmChatVO();
        vo.setModel(model);
        if (!enabled) {
            vo.setContent(buildFallbackAnswer(dto));
            vo.setFromLlm(false);
            return vo;
        }
        if (!StringUtils.hasText(apiKey)) {
            log.warn("MIMO_API_KEY not configured, using context fallback");
            vo.setContent(buildFallbackAnswer(dto));
            vo.setFromLlm(false);
            return vo;
        }
        try {
            String content = callChatCompletions(dto);
            vo.setContent(content);
            vo.setFromLlm(true);
            return vo;
        } catch (Exception e) {
            log.warn("LLM provider call failed: {}", e.getMessage());
            vo.setContent(buildFallbackAnswer(dto));
            vo.setFromLlm(false);
            return vo;
        }
    }

    private String callChatCompletions(LlmChatDTO dto) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("max_completion_tokens", maxCompletionTokens);
        ArrayNode messages = body.putArray("messages");
        if (StringUtils.hasText(dto.getSystemPrompt())) {
            ObjectNode sys = messages.addObject();
            sys.put("role", "system");
            sys.put("content", dto.getSystemPrompt());
        }
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", buildUserContent(dto));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        applyAuth(headers);

        String url = providerBaseUrl.replaceAll("/$", "") + "/chat/completions";
        String response = restTemplate.postForObject(url, new HttpEntity<>(body.toString(), headers), String.class);
        if (!StringUtils.hasText(response)) {
            throw new IllegalStateException("empty LLM response");
        }
        JsonNode root = objectMapper.readTree(response);
        JsonNode err = root.path("error");
        if (!err.isMissingNode() && err.has("message")) {
            throw new IllegalStateException(err.path("message").asText());
        }
        return root.path("choices").path(0).path("message").path("content").asText("");
    }

    private void applyAuth(HttpHeaders headers) {
        String key = apiKey.trim();
        if ("bearer".equalsIgnoreCase(authMode)) {
            headers.setBearerAuth(key);
        } else {
            // 小米 MiMo 官方文档使用 api-key 请求头
            headers.set("api-key", key);
        }
    }

    private String buildUserContent(LlmChatDTO dto) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(dto.getScenario())) {
            sb.append("场景: ").append(dto.getScenario()).append("\n\n");
        }
        if (dto.getContext() != null && !dto.getContext().isEmpty()) {
            sb.append("上下文:\n");
            for (Map.Entry<String, Object> e : dto.getContext().entrySet()) {
                sb.append("- ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
            }
            sb.append("\n");
        }
        sb.append(dto.getUserPrompt() != null ? dto.getUserPrompt() : "请根据上下文回答。");
        return sb.toString();
    }

    private String buildFallbackAnswer(LlmChatDTO dto) {
        if (dto.getContext() != null) {
            Object resume = dto.getContext().get("resumeText");
            Object fields = dto.getContext().get("extractedFields");
            String q = dto.getUserPrompt() != null ? dto.getUserPrompt().toLowerCase() : "";
            if (q.contains("公司") || q.contains("上家")) {
                if (fields instanceof Map) {
                    Object company = ((Map<?, ?>) fields).get("company");
                    if (company != null) {
                        return "上一家公司：" + company;
                    }
                }
            }
            if ((q.contains("学历") || q.contains("院校")) && fields instanceof Map) {
                Object school = ((Map<?, ?>) fields).get("school");
                if (school != null) {
                    return "毕业院校：" + school;
                }
            }
            if (resume != null && StringUtils.hasText(resume.toString())) {
                String text = resume.toString();
                return text.length() > 200 ? text.substring(0, 200) + "…" : text;
            }
        }
        return "LLM 服务暂不可用，请配置 MIMO_API_KEY 后重试。";
    }
}
