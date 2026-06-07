package com.recruitos.job.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Value Object for JD parse result
 */
public class JdParseResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Extracted tags with weights */
    private List<TagDTO> tags;

    /** Extracted entities (skills, tools, domains, etc.) */
    private Map<String, Object> entities;

    /** Raw text of the JD */
    private String rawText;

    // Getters and Setters

    public List<TagDTO> getTags() {
        return tags;
    }

    public void setTags(List<TagDTO> tags) {
        this.tags = tags;
    }

    public Map<String, Object> getEntities() {
        return entities;
    }

    public void setEntities(Map<String, Object> entities) {
        this.entities = entities;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }
}
