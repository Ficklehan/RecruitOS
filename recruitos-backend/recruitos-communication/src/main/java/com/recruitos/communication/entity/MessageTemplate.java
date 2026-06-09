package com.recruitos.communication.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

/**
 * Message template entity mapped to message_template table (init.sql schema).
 */
@TableName("message_template")
public class MessageTemplate extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Scene / channel type: INITIAL, CHASE, SMS, EMAIL, etc. */
    @TableField("scene")
    private String scene;

    @TableField("candidate_type")
    private String candidateType;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("priority")
    private Integer priority;

    @TableField("send_count")
    private Integer sendCount;

    @TableField("reply_count")
    private Integer replyCount;

    @TableField("resume_count")
    private Integer resumeCount;

    @TableField("hire_count")
    private Integer hireCount;

    @TableField("is_ab_test")
    private Integer isAbTest;

    @TableField("ab_group")
    private String abGroup;

    /** 1=enabled, 0=disabled */
    @TableField("status")
    private Integer status;

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public String getCandidateType() {
        return candidateType;
    }

    public void setCandidateType(String candidateType) {
        this.candidateType = candidateType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getSendCount() {
        return sendCount;
    }

    public void setSendCount(Integer sendCount) {
        this.sendCount = sendCount;
    }

    public Integer getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(Integer replyCount) {
        this.replyCount = replyCount;
    }

    public Integer getResumeCount() {
        return resumeCount;
    }

    public void setResumeCount(Integer resumeCount) {
        this.resumeCount = resumeCount;
    }

    public Integer getHireCount() {
        return hireCount;
    }

    public void setHireCount(Integer hireCount) {
        this.hireCount = hireCount;
    }

    public Integer getIsAbTest() {
        return isAbTest;
    }

    public void setIsAbTest(Integer isAbTest) {
        this.isAbTest = isAbTest;
    }

    public String getAbGroup() {
        return abGroup;
    }

    public void setAbGroup(String abGroup) {
        this.abGroup = abGroup;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
