package com.gkcontas.modulith.notification;

import java.util.List;
import java.util.UUID;

public interface NotificationLog {

    List<NotificationEntry> all();

    List<NotificationEntry> forOrder(UUID orderId);
}
