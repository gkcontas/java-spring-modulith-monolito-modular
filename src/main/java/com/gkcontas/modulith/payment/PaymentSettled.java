package com.gkcontas.modulith.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentSettled(UUID orderId, BigDecimal amount) {
}
