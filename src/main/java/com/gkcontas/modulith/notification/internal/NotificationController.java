package com.gkcontas.modulith.notification.internal;

import com.gkcontas.modulith.notification.NotificationEntry;
import com.gkcontas.modulith.notification.NotificationLog;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
class NotificationController {

    private final NotificationLog notifications;

    NotificationController(NotificationLog notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    List<NotificationEntry> all() {
        return notifications.all();
    }

    @GetMapping("/{orderId}")
    List<NotificationEntry> forOrder(@PathVariable UUID orderId) {
        return notifications.forOrder(orderId);
    }
}
