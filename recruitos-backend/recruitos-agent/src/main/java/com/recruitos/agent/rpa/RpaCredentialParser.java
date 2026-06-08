package com.recruitos.agent.rpa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitos.agent.entity.AgentAccount;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

@Component
public class RpaCredentialParser {

    @Resource
    private ObjectMapper objectMapper;

    public RpaCredential parse(AgentAccount account) {
        RpaCredential cred = new RpaCredential();
        if (account == null || !StringUtils.hasText(account.getEncryptedCredential())) {
            return cred;
        }
        try {
            return objectMapper.readValue(account.getEncryptedCredential(), RpaCredential.class);
        } catch (Exception e) {
            return cred;
        }
    }

    public String toJson(RpaCredential cred) {
        try {
            return objectMapper.writeValueAsString(cred);
        } catch (Exception e) {
            return "{\"authMode\":\"manual\"}";
        }
    }
}
