package com.gkcontas.modulith.payment;

import java.util.UUID;

public record PaymentFailed(UUID orderId, String reason) {
}
