package com.ailending.app.controller;

import com.ailending.app.controller.dto.orchestration.DocumentProcessingResponse;
import com.ailending.app.controller.dto.orchestration.LoanSubmissionRequest;
import com.ailending.app.controller.dto.orchestration.LoanSubmissionResponse;
import com.ailending.ingestion.service.IngestionRequest;
import com.ailending.orchestration.pipeline.DocumentProcessingPipeline;
import com.ailending.orchestration.pipeline.LoanSubmissionPipeline;
import com.ailending.ruleengine.model.RuleInput;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/lending/orchestration")
public class OrchestrationController {

    private final LoanSubmissionPipeline loanSubmissionPipeline;
    private final DocumentProcessingPipeline documentProcessingPipeline;

    public OrchestrationController(LoanSubmissionPipeline loanSubmissionPipeline,
                                    DocumentProcessingPipeline documentProcessingPipeline) {
        this.loanSubmissionPipeline    = loanSubmissionPipeline;
        this.documentProcessingPipeline = documentProcessingPipeline;
    }

    /**
     * Full loan submission pipeline: create loan → evaluate rules → audit.
     * Advisory only — AI never makes autonomous decisions (ADR-003).
     */
    @PostMapping("/loans")
    @ResponseStatus(HttpStatus.CREATED)
    public LoanSubmissionResponse submitLoan(@RequestBody LoanSubmissionRequest request) {
        RuleInput ruleInput = RuleInput.builder()
                .loanId(request.getCustomerId())     // loanId not yet assigned; use customerId as placeholder
                .customerId(request.getCustomerId())
                .monthlyIncome(request.getMonthlyIncome())
                .requestedAmount(request.getAmount())
                .existingMonthlyDebt(
                        request.getExistingMonthlyDebt() != null
                        ? request.getExistingMonthlyDebt()
                        : BigDecimal.ZERO)
                .creditScore(request.getCreditScore())
                .kycVerified(request.isKycVerified())
                .amlClear(request.isAmlClear())
                .build();

        return LoanSubmissionResponse.from(
                loanSubmissionPipeline.process(
                        request.getCustomerId(), request.getAmount(), request.getCurrency(), ruleInput));
    }

    /**
     * Full document processing pipeline: ingest → document intelligence → audit.
     */
    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentProcessingResponse processDocument(
            @RequestParam("file")             MultipartFile file,
            @RequestParam("loanId")           String loanId,
            @RequestParam("customerId")       String customerId,
            @RequestParam("documentType")     String documentType,
            @RequestParam("sourceDocumentId") String sourceDocumentId,
            @RequestParam(value = "policyVersion", required = false) String policyVersion
    ) throws IOException {

        String mimeType = file.getContentType() != null
                ? file.getContentType()
                : "application/octet-stream";

        IngestionRequest request = IngestionRequest.builder()
                .content(file.getBytes())
                .mimeType(mimeType)
                .loanId(loanId)
                .customerId(customerId)
                .documentType(documentType)
                .sourceDocumentId(sourceDocumentId)
                .policyVersion(policyVersion)
                .build();

        return DocumentProcessingResponse.from(documentProcessingPipeline.process(request));
    }
}
