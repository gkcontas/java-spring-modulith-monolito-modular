package com.gkcontas.modulith.integration;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderFlowIntegrationTest extends IntegrationTestBase {

    @Test
    void confirmsAnOrderThatPassesEveryStep() throws Exception {
        UUID id = placeOrder("KEYBOARD-01", 2, "450.00");

        JsonNode confirmed = awaitStatus(id, "CONFIRMED");

        assertThat(confirmed.get("statusReason").isNull()).isTrue();
        // Four modules took part and none of them called another.
        assertThat(eventsOf(id)).containsExactly("OrderPlaced", "StockReserved", "PaymentSettled");
    }

    @Test
    void reservesTheStockItConfirmed() throws Exception {
        int before = availableOf("MOUSE-02");

        UUID id = placeOrder("MOUSE-02", 3, "120.00");
        awaitStatus(id, "CONFIRMED");

        assertThat(availableOf("MOUSE-02")).isEqualTo(before - 3);
    }

    @Test
    void rejectsAnOrderWithoutEnoughStock() throws Exception {
        UUID id = placeOrder("MONITOR-03", 99, "2500.00");

        JsonNode rejected = awaitStatus(id, "REJECTED");

        assertThat(rejected.get("statusReason").asText()).startsWith("out of stock");
        // The chain stops at the first refusal: payment never saw this order.
        assertThat(eventsOf(id)).containsExactly("OrderPlaced", "StockRejected");
    }

    @Test
    void rejectsAnOrderThePaymentDeclines() throws Exception {
        UUID id = placeOrder("LAPTOP-04", 1, "15000.00");

        JsonNode rejected = awaitStatus(id, "REJECTED");

        assertThat(rejected.get("statusReason").asText()).contains("payment failed");
        assertThat(eventsOf(id)).containsExactly("OrderPlaced", "StockReserved", "PaymentFailed");
    }

    @Test
    void keepsEachModulesViewOfTheSameOrder() throws Exception {
        UUID id = placeOrder("KEYBOARD-01", 1, "230.00");
        awaitStatus(id, "CONFIRMED");

        // Three modules hold three different records about one order, joined by its id and
        // by nothing else — no foreign key crosses a module boundary.
        assertThat(json("/orders/{id}", id).get("id").asText()).isEqualTo(id.toString());
        assertThat(json("/payments")).anySatisfy(payment ->
                assertThat(payment.get("orderId").asText()).isEqualTo(id.toString()));
        assertThat(json("/notifications/{id}", id)).isNotEmpty();
    }

    private List<String> eventsOf(UUID orderId) {
        List<String> events = new ArrayList<>();
        json("/notifications/{id}", orderId).forEach(entry -> events.add(entry.get("event").asText()));
        return events;
    }

    private int availableOf(String sku) {
        for (JsonNode level : json("/stock")) {
            if (sku.equals(level.get("sku").asText())) {
                return level.get("available").asInt();
            }
        }
        throw new AssertionError("No stock level for " + sku);
    }
}
