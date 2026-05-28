package com.ailending.app.controller.dto.policy;

public class PolicyQueryRequest {
    private String question;
    private String policyVersion;

    public String getQuestion()             { return question; }
    public void setQuestion(String v)       { this.question = v; }
    public String getPolicyVersion()        { return policyVersion; }
    public void setPolicyVersion(String v)  { this.policyVersion = v; }
}
