package com.gkcontas.modulith.order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The module's public API: an interface, with the implementation in {@code internal}.
 *
 * <p>Spring Modulith treats types in a module's root package as its API and everything in
 * a nested package as private to it. The verification test refuses a reference from
 * another module to {@code order.internal}, which is what keeps this interface from being
 * a suggestion.
 */
public interface OrderManagement {

    OrderDetails place(PlaceOrder command);

    void confirm(UUID orderId);

    void reject(UUID orderId, String reason);

    Optional<OrderDetails> findById(UUID orderId);

    List<OrderDetails> findAll();
}
