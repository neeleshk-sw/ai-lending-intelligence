package com.ailending.app.controller;

import com.ailending.app.controller.dto.ingestion.IngestionResponse;
import com.ailending.ingestion.service.DocumentIngestor;
import com.ailending.ingestion.service.IngestionRequest;
import com.ailending.ingestion.service.IngestionResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * REST endpoint for document ingestion.
 *
 * <p>Receives a raw document file plus loan metadata, runs it through the
 * ingestion pipeline (parse → chunk → embed → store), and returns the result.
 *
 * <p>All data stays within organization-controlled infrastructure — no bytes
 * are forwarded to external AI providers (NFR-7, ADR-006).
 */
@RestController
@RequestMapping("/api/lending")
public class IngestionController {

    private final DocumentIngestor documentIngestor;

    public IngestionController(DocumentIngestor documentIngestor) {
        this.documentIngestor = documentIngestor;
    }

    /**
     * Ingest a loan-related document.
     *
     * <p>The multipart file content is passed verbatim to the ingestion pipeline.
     * The {@code Content-Type} header of the uploaded part is used as the MIME type hint.
     *
     * @param file             raw document bytes (PDF, DOCX, TXT, etc.)
     * @param loanId           identifier of the associated loan
     * @param customerId       identifier of the borrower
     * @param documentType     one of the supported type strings (e.g. {@code SALARY_SLIP})
     * @param sourceDocumentId caller-assigned stable identifier for this document
     * @param policyVersion    optional policy version tag
     * @return ingestion result: chunk count, status, and elapsed time
     * @throws IOException if the uploaded file cannot be read
     */
    @PostMapping(value = "/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IngestionResponse ingest(
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

        IngestionResult result = documentIngestor.ingest(request);
        return IngestionResponse.from(result);
    }
}
