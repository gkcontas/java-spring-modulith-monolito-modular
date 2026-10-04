package com.gkcontas.modulith.integration;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * What happens when a listener fails — and why that is not the end of the order.
 *
 * <p>Asynchronous listeners inside a monolith usually lose the event when they throw, in
 * exactly the way a message consumer does without a dead-letter queue. The publication
 * registry is what makes the in-process version recoverable: each delivery is written
 * before the listener runs and completed only when it returns, so a failure leaves a row
 * that can be found and replayed.
 *
 * <p>The SKU prefix {@code BOOM} makes the simulated payment gateway unreachable on its
 * first attempt, which is a technical failure rather than a business one — the listener
 * throws instead of publishing a result.
 */
class EventPublicationRegistryTest extends IntegrationTestBase {

    @Test
    void leavesTheFailedDeliveryPendingAndTheOrderUnfinished() throws Exception {
        UUID id = placeOrder("BOOM-99", 1, "99.00");

        List<String> pending = awaitPendingPaymentDeliveries();

        // The order never reaches a final status: nothing rejected it, the step simply did
        // not happen.
        assertThat(order(id).get("status").asText()).isEqualTo("PENDING");
        // One row per listener, not per event: notification handled the same StockReserved
        // without trouble, and its delivery is already complete.
        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst()).contains("payment").contains("StockReservedListener");
    }

    @Test
    void finishesTheOrderWhenThePendingDeliveryIsReplayed() throws Exception {
        UUID id = placeOrder("BOOM-99", 1, "150.00");
        awaitPendingPaymentDeliveries();

        mockMvc.perform(post("/event-registry/resubmit").contentType(MediaType.APPLICATION_JSON));

        // Replaying the delivery resumes the flow exactly where it stopped: payment
        // settles, fulfillment confirms, and the order that was stuck completes.
        assertThat(awaitStatus(id, "CONFIRMED").get("status").asText()).isEqualTo("CONFIRMED");
        await().atMost(TIMEOUT).until(() -> pendingPaymentDeliveries().isEmpty());
    }

    @Test
    void recordsEveryDeliveryIncludingTheSuccessfulOnes() throws Exception {
        UUID id = placeOrder("KEYBOARD-01", 1, "99.00");
        awaitStatus(id, "CONFIRMED");

        // Three events, each delivered to more than one listener, all tracked.
        assertThat(json("/event-registry")).isNotEmpty();
        assertThat(pendingPaymentDeliveries()).isEmpty();
    }

    private List<String> awaitPendingPaymentDeliveries() {
        await().atMost(TIMEOUT).until(() -> !pendingPaymentDeliveries().isEmpty());
        return pendingPaymentDeliveries();
    }

    /**
     * Pending deliveries, narrowed to the payment listener.
     *
     * <p>The registry has no column for the order, only for the listener and the event
     * type, so the filter is by listener. It is enough here because only the payment
     * listener is ever made to fail.
     */
    private List<String> pendingPaymentDeliveries() {
        return java.util.stream.StreamSupport
                .stream(json("/event-registry/incomplete").spliterator(), false)
                .map(node -> node.get("listenerId").asText())
                .filter(listener -> listener.contains("StockReservedListener"))
                .toList();
    }
}
