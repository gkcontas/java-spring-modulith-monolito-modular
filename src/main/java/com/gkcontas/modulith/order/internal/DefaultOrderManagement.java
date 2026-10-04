package com.gkcontas.modulith.order.internal;

import com.gkcontas.modulith.order.OrderDetails;
import com.gkcontas.modulith.order.OrderManagement;
import com.gkcontas.modulith.order.OrderPlaced;
import com.gkcontas.modulith.order.OrderStatus;
import com.gkcontas.modulith.order.PlaceOrder;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the order and announces it. Nothing else.
 *
 * <p>The publication happens inside the same transaction as the insert, and the listeners
 * only run after it commits — that is what {@code @ApplicationModuleListener} buys. So
 * there is no window in which {@code inventory} reserves stock for an order that was
 * rolled back, and no window in which the order exists and nobody was told.
 */
@Service
class DefaultOrderManagement implements OrderManagement {

    private final OrderRepository repository;
    private final ApplicationEventPublisher events;

    DefaultOrderManagement(OrderRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    @Override
    @Transactional
    public OrderDetails place(PlaceOrder command) {
        OrderEntity entity = new OrderEntity(UUID.randomUUID(), command.sku(), command.quantity(),
                command.amount(), command.customerEmail(), OrderStatus.PENDING, null, Instant.now());
        repository.save(entity);

        events.publishEvent(new OrderPlaced(entity.getId(), entity.getSku(), entity.getQuantity(),
                entity.getAmount(), entity.getCustomerEmail()));

        return toDetails(entity);
    }

    @Override
    @Transactional
    public void confirm(UUID orderId) {
        OrderEntity entity = require(orderId);
        entity.setStatus(OrderStatus.CONFIRMED);
        entity.setStatusReason(null);
    }

    @Override
    @Transactional
    public void reject(UUID orderId, String reason) {
        OrderEntity entity = require(orderId);
        entity.setStatus(OrderStatus.REJECTED);
        entity.setStatusReason(reason);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderDetails> findById(UUID orderId) {
        return repository.findById(orderId).map(DefaultOrderManagement::toDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDetails> findAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(OrderEntity::getPlacedAt).reversed())
                .map(DefaultOrderManagement::toDetails)
                .toList();
    }

    private OrderEntity require(UUID orderId) {
        return repository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("No order with id " + orderId));
    }

    private static OrderDetails toDetails(OrderEntity entity) {
        return new OrderDetails(entity.getId(), entity.getSku(), entity.getQuantity(),
                entity.getAmount(), entity.getCustomerEmail(), entity.getStatus(),
                entity.getStatusReason(), entity.getPlacedAt());
    }
}
