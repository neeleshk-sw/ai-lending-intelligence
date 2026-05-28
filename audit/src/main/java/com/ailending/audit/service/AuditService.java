package com.ailending.audit.service;

import com.ailending.audit.model.AuditRecord;

import java.util.List;

public interface AuditService {

    void record(AuditRecord record);

    List<AuditRecord> findByLoanId(String loanId);

    List<AuditRecord> findByCustomerId(String customerId);
}
