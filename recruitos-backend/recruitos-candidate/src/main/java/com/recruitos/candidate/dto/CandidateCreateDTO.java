package com.recruitos.candidate.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for creating or updating a candidate
 */
public class CandidateCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String phone;
    private String email;
    private String gender;
    private String currentCompany;
    private String currentTitle;
    private Integer workYears;
    private String education;
    private String school;
    private String major;
    private BigDecimal expectedSalary;
    private String workLocation;
    private String source;
    private String sourceDetail;

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
}
