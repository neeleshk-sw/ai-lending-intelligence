package com.ailending.app.controller;

import com.ailending.app.controller.dto.policy.PolicyIngestRequest;
import com.ailending.app.controller.dto.policy.PolicyIngestResponse;
import com.ailending.app.controller.dto.policy.PolicyQueryRequest;
import com.ailending.app.controller.dto.query.QueryResponse;
import com.ailending.policy.service.PolicyIngestorService;
import com.ailending.policy.service.PolicyQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lending/policies")
public class PolicyController {

    private final PolicyIngestorService policyIngestorService;
    private final PolicyQueryService policyQueryService;

    public PolicyController(PolicyIngestorService policyIngestorService,
                            PolicyQueryService policyQueryService) {
        this.policyIngestorService = policyIngestorService;
        this.policyQueryService    = policyQueryService;
    }

    @PostMapping("/ingest")
    @ResponseStatus(HttpStatus.CREATED)
    public PolicyIngestResponse ingest(@RequestBody PolicyIngestRequest request) {
        return PolicyIngestResponse.from(
                policyIngestorService.ingest(
                        request.getPolicyName(), request.getVersion(), request.getTextContent()));
    }

    @PostMapping("/query")
    public QueryResponse query(@RequestBody PolicyQueryRequest request) {
        return QueryResponse.from(
                policyQueryService.query(request.getQuestion(), request.getPolicyVersion()));
    }
}
