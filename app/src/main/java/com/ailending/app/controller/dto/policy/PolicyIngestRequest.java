package com.ailending.app.controller.dto.policy;

public class PolicyIngestRequest {
    private String policyName;
    private String version;
    private String textContent;

    public String getPolicyName()           { return policyName; }
    public void setPolicyName(String v)     { this.policyName = v; }
    public String getVersion()              { return version; }
    public void setVersion(String v)        { this.version = v; }
    public String getTextContent()          { return textContent; }
    public void setTextContent(String v)    { this.textContent = v; }
}
