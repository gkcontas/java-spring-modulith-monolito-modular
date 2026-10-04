package com.gkcontas.modulith.inventory;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Carries the amount even though inventory does not care about money.
 *
 * <p>{@code payment} reacts to this event and needs to know how much to charge. The
 * alternative is for it to call back into {@code order} and ask — which adds a dependency
 * in the opposite direction and turns a one-way flow into a conversation. Copying the
 * value forward is the cheaper trade, and it is also what makes each event a complete,
 * self-contained fact.
 */
public record StockReserved(UUID orderId, String sku, int quantity, BigDecimal amount) {
}
