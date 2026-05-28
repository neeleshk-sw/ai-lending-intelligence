package com.ailending.policy.service;

import com.ailending.policy.model.PolicyVersion;

public interface PolicyIngestorService {

    PolicyVersion ingest(String policyName, String version, String textContent);
}
