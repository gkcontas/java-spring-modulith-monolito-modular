package com.gkcontas.modulith.notification.internal;

import com.gkcontas.modulith.inventory.StockRejected;
import com.gkcontas.modulith.inventory.StockReserved;
import com.gkcontas.modulith.notification.NotificationEntry;
import com.gkcontas.modulith.notification.NotificationLog;
import com.gkcontas.modulith.order.OrderPlaced;
import com.gkcontas.modulith.payment.PaymentFailed;
import com.gkcontas.modulith.payment.PaymentSettled;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A module that only consumes — and the clearest demonstration of what events buy.
 *
 * <p>Nothing was changed anywhere else to add it. {@code order} does not know it exists,
 * {@code payment} does not know it exists, and removing it tomorrow breaks nobody. That is
 * the difference between publishing a fact and calling a method: a direct call would have
 * required editing four modules to insert this one.
 *
 * <p>Several listeners here react to the same events as {@code fulfillment}. Each one gets
 * its own entry in the publication registry and its own transaction, so one failing has no
 * effect on the other.
 */
@Service
class NotificationListener implements NotificationLog {

    private final NotificationRepository repository;

    NotificationListener(NotificationRepository repository) {
        this.repository = repository;
    }

    @ApplicationModuleListener
    void on(OrderPlaced event) {
        record(event.orderId(), "OrderPlaced",
                "Order received for %d x %s".formatted(event.quantity(), event.sku()));
    }

    @ApplicationModuleListener
    void on(StockReserved event) {
        record(event.orderId(), "StockReserved",
                "Stock reserved for %d x %s".formatted(event.quantity(), event.sku()));
    }

    @ApplicationModuleListener
    void on(StockRejected event) {
        record(event.orderId(), "StockRejected", "Stock unavailable: " + event.reason());
    }

    @ApplicationModuleListener
    void on(PaymentSettled event) {
        record(event.orderId(), "PaymentSettled", "Charged " + event.amount());
    }

    @ApplicationModuleListener
    void on(PaymentFailed event) {
        record(event.orderId(), "PaymentFailed", event.reason());
    }

    private void record(UUID orderId, String event, String message) {
        repository.save(new NotificationEntity(orderId, event, message, Instant.now()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationEntry> all() {
        return repository.findAll().stream().map(NotificationListener::toEntry).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationEntry> forOrder(UUID orderId) {
        return repository.findByOrderIdOrderByRecordedAtAsc(orderId).stream()
                .map(NotificationListener::toEntry).toList();
    }

    private static NotificationEntry toEntry(NotificationEntity entity) {
        return new NotificationEntry(entity.getOrderId(), entity.getEvent(), entity.getMessage(),
                entity.getRecordedAt());
    }
}
