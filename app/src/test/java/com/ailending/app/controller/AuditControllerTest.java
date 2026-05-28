package com.ailending.app.controller;

import com.ailending.audit.model.AuditEventType;
import com.ailending.audit.model.AuditRecord;
import com.ailending.audit.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuditControllerTest {

    private AuditService auditService;
    private MockMvc mockMvc;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        auditService = mock(AuditService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuditController(auditService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void record_returnsCreatedWithAuditId() throws Exception {
        Map<String, Object> body = Map.of(
                "loanId", "loan-1",
                "customerId", "cust-1",
                "eventType", "AI_REVIEW_COMPLETED",
                "performedBy", "system"
        );

        mockMvc.perform(post("/api/lending/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.auditId").isNotEmpty())
                .andExpect(jsonPath("$.loanId").value("loan-1"))
                .andExpect(jsonPath("$.eventType").value("AI_REVIEW_COMPLETED"));

        verify(auditService).record(any(AuditRecord.class));
    }

    @Test
    void getByLoanId_returnsListOfRecords() throws Exception {
        AuditRecord r = AuditRecord.builder()
                .loanId("loan-2")
                .customerId("cust-2")
                .eventType(AuditEventType.STATUS_TRANSITION)
                .performedBy("system")
                .build();

        when(auditService.findByLoanId("loan-2")).thenReturn(List.of(r));

        mockMvc.perform(get("/api/lending/audit/loan-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].loanId").value("loan-2"))
                .andExpect(jsonPath("$[0].eventType").value("STATUS_TRANSITION"));
    }

    @Test
    void getByLoanId_noRecords_returnsEmptyArray() throws Exception {
        when(auditService.findByLoanId("unknown")).thenReturn(List.of());

        mockMvc.perform(get("/api/lending/audit/unknown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void record_invalidEventType_returnsBadRequest() throws Exception {
        Map<String, Object> body = Map.of(
                "loanId", "loan-1",
                "customerId", "cust-1",
                "eventType", "INVALID_TYPE"
        );

        mockMvc.perform(post("/api/lending/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity());
    }
}
