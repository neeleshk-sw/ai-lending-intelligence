package com.ailending.audit.service;

import com.ailending.audit.exception.AuditException;
import com.ailending.audit.model.AuditRecord;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class AuditServiceImpl implements AuditService {

    private final CopyOnWriteArrayList<AuditRecord> store = new CopyOnWriteArrayList<>();

    @Override
    public void record(AuditRecord record) {
        if (record == null) {
            throw new AuditException("AuditRecord must not be null");
        }
        store.add(record);
    }

    @Override
    public List<AuditRecord> findByLoanId(String loanId) {
        return store.stream()
                .filter(r -> loanId != null && loanId.equals(r.getLoanId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditRecord> findByCustomerId(String customerId) {
        return store.stream()
                .filter(r -> customerId != null && customerId.equals(r.getCustomerId()))
                .collect(Collectors.toList());
    }
}
