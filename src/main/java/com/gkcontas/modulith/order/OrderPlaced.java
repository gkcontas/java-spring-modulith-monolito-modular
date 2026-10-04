package com.gkcontas.modulith.order;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The event that starts the flow, and the only thing other modules know about an order.
 *
 * <p>It lives in the module's public package on purpose: an event is part of the API a
 * module offers, as much as an interface is. Everything a consumer needs travels inside
 * it, so {@code inventory} never has to ask {@code order} for anything — and the day this
 * module moves out of the monolith, the consumers keep working against the same payload.
 *
 * <p>Note what is <em>not</em> here: the {@code Order} entity. Publishing the entity would
 * put a JPA class, with its lazy associations and its identity, into every listener's
 * transaction. A record of plain values crosses the boundary cleanly and can be serialised
 * into the publication registry.
 */
public record OrderPlaced(
        UUID orderId,
        String sku,
        int quantity,
        BigDecimal amount,
        String customerEmail) {
}
