package com.ailending.audit.service;

import com.ailending.audit.exception.AuditException;
import com.ailending.audit.model.AuditEventType;
import com.ailending.audit.model.AuditRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuditServiceImplTest {

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditServiceImpl();
    }

    // -----------------------------------------------------------------------
    // record + findByLoanId
    // -----------------------------------------------------------------------

    @Test
    void recordAndFindByLoanId_returnsMatchingRecords() {
        AuditRecord r = baseRecord("loan-1", "cust-1").build();
        auditService.record(r);

        List<AuditRecord> results = auditService.findByLoanId("loan-1");

        assertEquals(1, results.size());
        assertEquals(r.getAuditId(), results.get(0).getAuditId());
    }

    @Test
    void findByLoanId_multipleEvents_returnedInInsertionOrder() {
        auditService.record(baseRecord("loan-2", "cust-1").eventType(AuditEventType.STATUS_TRANSITION).build());
        auditService.record(baseRecord("loan-2", "cust-1").eventType(AuditEventType.AI_REVIEW_COMPLETED).build());
        auditService.record(baseRecord("loan-2", "cust-1").eventType(AuditEventType.UNDERWRITER_DECISION).build());

        List<AuditRecord> results = auditService.findByLoanId("loan-2");

        assertEquals(3, results.size(), "All three events should be returned");
        assertEquals(AuditEventType.STATUS_TRANSITION,   results.get(0).getEventType());
        assertEquals(AuditEventType.AI_REVIEW_COMPLETED, results.get(1).getEventType());
        assertEquals(AuditEventType.UNDERWRITER_DECISION, results.get(2).getEventType());
    }

    @Test
    void findByLoanId_unknownId_returnsEmptyList() {
        auditService.record(baseRecord("loan-3", "cust-1").build());

        List<AuditRecord> results = auditService.findByLoanId("unknown-loan");

        assertTrue(results.isEmpty());
    }

    // -----------------------------------------------------------------------
    // findByCustomerId
    // -----------------------------------------------------------------------

    @Test
    void findByCustomerId_returnsOnlyMatchingRecords() {
        auditService.record(baseRecord("loan-A", "cust-X").build());
        auditService.record(baseRecord("loan-B", "cust-Y").build());
        auditService.record(baseRecord("loan-C", "cust-X").build());

        List<AuditRecord> results = auditService.findByCustomerId("cust-X");

        assertEquals(2, results.size(), "Only records for cust-X should be returned");
        assertTrue(results.stream().allMatch(r -> "cust-X".equals(r.getCustomerId())));
    }

    @Test
    void findByCustomerId_unknownId_returnsEmptyList() {
        List<AuditRecord> results = auditService.findByCustomerId("no-such-customer");
        assertTrue(results.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Validation
    // -----------------------------------------------------------------------

    @Test
    void record_nullRecord_throwsAuditException() {
        assertThrows(AuditException.class, () -> auditService.record(null));
    }

    @Test
    void builder_nullLoanId_throwsAuditException() {
        assertThrows(AuditException.class, () ->
                AuditRecord.builder()
                        .customerId("cust-1")
                        .eventType(AuditEventType.AI_REVIEW_COMPLETED)
                        .build()
        );
    }

    @Test
    void builder_nullEventType_throwsAuditException() {
        assertThrows(AuditException.class, () ->
                AuditRecord.builder()
                        .loanId("loan-1")
                        .customerId("cust-1")
                        .build()
        );
    }

    // -----------------------------------------------------------------------
    // Immutability — builder isolation
    // -----------------------------------------------------------------------

    @Test
    void builderProducesSeparateInstances() {
        AuditRecord.Builder builder = AuditRecord.builder()
                .loanId("loan-1")
                .customerId("cust-1")
                .eventType(AuditEventType.STATUS_TRANSITION);

        AuditRecord r1 = builder.build();
        AuditRecord r2 = builder.build();

        assertNotEquals(r1.getAuditId(), r2.getAuditId(), "Each build() call should generate a new UUID");
    }

    // -----------------------------------------------------------------------
    // Optional fields
    // -----------------------------------------------------------------------

    @Test
    void recordWithAllOptionalFields_storedAndRetrievedCorrectly() {
        AuditRecord r = AuditRecord.builder()
                .loanId("loan-5")
                .customerId("cust-5")
                .eventType(AuditEventType.UNDERWRITER_DECISION)
                .performedBy("underwriter-007")
                .aiGeneratedText("Risk is moderate.")
                .confidenceScore(0.82)
                .modelUsed("llama3:8b")
                .aiDurationMs(1500L)
                .sourceReferenceCount(3)
                .decision("APPROVED")
                .notes("Verified income manually")
                .build();

        auditService.record(r);
        List<AuditRecord> results = auditService.findByLoanId("loan-5");

        assertEquals(1, results.size());
        AuditRecord stored = results.get(0);
        assertEquals("underwriter-007", stored.getPerformedBy());
        assertEquals(0.82, stored.getConfidenceScore());
        assertEquals("llama3:8b", stored.getModelUsed());
        assertEquals(1500L, stored.getAiDurationMs());
        assertEquals(3, stored.getSourceReferenceCount());
        assertEquals("APPROVED", stored.getDecision());
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private AuditRecord.Builder baseRecord(String loanId, String customerId) {
        return AuditRecord.builder()
                .loanId(loanId)
                .customerId(customerId)
                .eventType(AuditEventType.AI_REVIEW_COMPLETED)
                .performedBy("system");
    }
}
