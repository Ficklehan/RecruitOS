package com.recruitos.candidate.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Candidate entity mapped to candidate table
 */
@TableName("candidate")
public class Candidate extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Candidate name */
    private String name;

    /** Phone number */
    private String phone;

    /** Email address */
    private String email;

    /** Gender: MALE/FEMALE/UNKNOWN */
    private String gender;

    /** Birth date */
    private LocalDate birthDate;

    /** Current company */
    private String currentCompany;

    /** Current job title */
    private String currentTitle;

    /** Years of work experience */
    private Integer workYears;

    /** Education level */
    private String education;

    /** School name */
    private String school;

    /** Major */
    private String major;

    /** Expected salary */
    private BigDecimal expectedSalary;

    /** Preferred work location */
    private String workLocation;

    /** Candidate source */
    private String source;

    /** Source detail */
    private String sourceDetail;

    /** Status */
    private String status;

    /** Tags (JSON string) */
    private String tags;

    /** Embedding vector ID for AI matching */
    private String embeddingVectorId;

    /** Remark */
    private String remark;

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getCurrentCompany() {
        return currentCompany;
    }

    public void setCurrentCompany(String currentCompany) {
        this.currentCompany = currentCompany;
    }

    public String getCurrentTitle() {
        return currentTitle;
    }

    public void setCurrentTitle(String currentTitle) {
        this.currentTitle = currentTitle;
    }

    public Integer getWorkYears() {
        return workYears;
    }

    public void setWorkYears(Integer workYears) {
        this.workYears = workYears;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public BigDecimal getExpectedSalary() {
        return expectedSalary;
    }

    public void setExpectedSalary(BigDecimal expectedSalary) {
        this.expectedSalary = expectedSalary;
    }

    public String getWorkLocation() {
        return workLocation;
    }

    public void setWorkLocation(String workLocation) {
        this.workLocation = workLocation;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceDetail() {
        return sourceDetail;
    }

    public void setSourceDetail(String sourceDetail) {
        this.sourceDetail = sourceDetail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getEmbeddingVectorId() {
        return embeddingVectorId;
    }

    public void setEmbeddingVectorId(String embeddingVectorId) {
        this.embeddingVectorId = embeddingVectorId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
