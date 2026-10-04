package com.gkcontas.modulith.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderDetails(
        UUID id,
        String sku,
        int quantity,
        BigDecimal amount,
        String customerEmail,
        OrderStatus status,
        String statusReason,
        Instant placedAt) {
}
