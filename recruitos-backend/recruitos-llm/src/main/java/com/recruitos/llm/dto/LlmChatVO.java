package com.recruitos.llm.dto;

public class LlmChatVO {

    private String content;
    private String model;
    private boolean fromLlm;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isFromLlm() {
        return fromLlm;
    }

    public void setFromLlm(boolean fromLlm) {
        this.fromLlm = fromLlm;
    }
}
