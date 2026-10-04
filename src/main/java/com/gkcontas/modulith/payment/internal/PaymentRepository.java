package com.gkcontas.modulith.payment.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {
}
