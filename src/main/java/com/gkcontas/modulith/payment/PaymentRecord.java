package com.gkcontas.modulith.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentRecord(UUID orderId, BigDecimal amount, String outcome, Instant settledAt) {
}
