package com.gkcontas.modulith.payment.internal;

import com.gkcontas.modulith.payment.PaymentManagement;
import com.gkcontas.modulith.payment.PaymentRecord;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DefaultPaymentManagement implements PaymentManagement {

    private final PaymentRepository repository;

    DefaultPaymentManagement(PaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentRecord> history() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(PaymentEntity::getSettledAt).reversed())
                .map(entity -> new PaymentRecord(entity.getOrderId(), entity.getAmount(),
                        entity.getOutcome(), entity.getSettledAt()))
                .toList();
    }
}
