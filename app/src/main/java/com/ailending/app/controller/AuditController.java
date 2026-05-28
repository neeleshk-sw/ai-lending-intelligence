package com.ailending.app.controller;

import com.ailending.audit.model.AuditEventType;
import com.ailending.audit.model.AuditRecord;
import com.ailending.audit.service.AuditService;
import com.ailending.app.controller.dto.audit.AuditRecordResponse;
import com.ailending.app.controller.dto.audit.RecordAuditRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/lending/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuditRecordResponse record(@RequestBody RecordAuditRequest request) {
        AuditRecord record = AuditRecord.builder()
                .loanId(request.getLoanId())
                .customerId(request.getCustomerId())
                .eventType(AuditEventType.valueOf(request.getEventType()))
                .performedBy(request.getPerformedBy())
                .aiGeneratedText(request.getAiGeneratedText())
                .confidenceScore(request.getConfidenceScore())
                .modelUsed(request.getModelUsed())
                .aiDurationMs(request.getAiDurationMs())
                .sourceReferenceCount(request.getSourceReferenceCount())
                .decision(request.getDecision())
                .notes(request.getNotes())
                .build();

        auditService.record(record);
        return AuditRecordResponse.from(record);
    }

    @GetMapping("/{loanId}")
    public List<AuditRecordResponse> getByLoanId(@PathVariable String loanId) {
        return auditService.findByLoanId(loanId).stream()
                .map(AuditRecordResponse::from)
                .collect(Collectors.toList());
    }
}
