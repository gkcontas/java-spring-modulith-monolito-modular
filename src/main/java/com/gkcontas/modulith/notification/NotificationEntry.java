package com.gkcontas.modulith.notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationEntry(UUID orderId, String event, String message, Instant recordedAt) {
}
