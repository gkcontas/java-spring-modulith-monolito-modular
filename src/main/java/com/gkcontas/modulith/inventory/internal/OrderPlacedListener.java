package com.gkcontas.modulith.inventory.internal;

import com.gkcontas.modulith.inventory.StockRejected;
import com.gkcontas.modulith.inventory.StockReserved;
import com.gkcontas.modulith.order.OrderPlaced;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * How one module reacts to another without either of them holding a reference.
 *
 * <p>{@code @ApplicationModuleListener} is three annotations in one: {@code @Async},
 * {@code @Transactional(propagation = REQUIRES_NEW)} and
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)}. Each half matters.
 *
 * <ul>
 *   <li><b>After commit</b> — the order is already durable when this runs, so stock is
 *       never reserved for an order that rolled back.
 *   <li><b>New transaction</b> — a failure here rolls back the reservation and nothing
 *       else. The order stays placed; it simply never advances.
 *   <li><b>Asynchronous</b> — the caller's request returns without waiting for the whole
 *       chain, and a slow listener does not become a slow endpoint.
 * </ul>
 *
 * <p>The cost of asynchronous delivery is that a crash between commit and delivery would
 * lose the event. That is what the publication registry prevents: each delivery is written
 * to the database first and marked complete only when the listener returns.
 */
@Component
class OrderPlacedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderPlacedListener.class);

    private final StockRepository repository;
    private final ApplicationEventPublisher events;

    OrderPlacedListener(StockRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    @ApplicationModuleListener
    void on(OrderPlaced event) {
        Optional<StockItemEntity> item = repository.findBySku(event.sku());

        if (item.isEmpty()) {
            publishRejection(event, "unknown sku");
            return;
        }
        StockItemEntity stock = item.get();
        if (stock.getAvailable() < event.quantity()) {
            publishRejection(event, "only %d available".formatted(stock.getAvailable()));
            return;
        }

        stock.setAvailable(stock.getAvailable() - event.quantity());
        stock.setReserved(stock.getReserved() + event.quantity());

        log.info("Reserved {} of {} for order {}", event.quantity(), event.sku(), event.orderId());
        events.publishEvent(new StockReserved(event.orderId(), event.sku(), event.quantity(),
                event.amount()));
    }

    private void publishRejection(OrderPlaced event, String reason) {
        log.info("Rejected order {}: {}", event.orderId(), reason);
        events.publishEvent(new StockRejected(event.orderId(), event.sku(), reason));
    }
}
