package com.gkcontas.modulith.fulfillment.internal;

import com.gkcontas.modulith.inventory.StockRejected;
import com.gkcontas.modulith.order.OrderManagement;
import com.gkcontas.modulith.payment.PaymentFailed;
import com.gkcontas.modulith.payment.PaymentSettled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * The only place that knows what the whole flow means.
 *
 * <p>Each of the other modules answers one question — is there stock, did the charge go
 * through — and none of them decides what that implies for the order. That decision is
 * here, which is why adding a step later (a fraud check, say) changes this class and
 * nothing else.
 */
@Component
class OrderOutcomeListener {

    private static final Logger log = LoggerFactory.getLogger(OrderOutcomeListener.class);

    private final OrderManagement orders;

    OrderOutcomeListener(OrderManagement orders) {
        this.orders = orders;
    }

    @ApplicationModuleListener
    void on(StockRejected event) {
        log.info("Rejecting order {} for lack of stock", event.orderId());
        orders.reject(event.orderId(), "out of stock: " + event.reason());
    }

    @ApplicationModuleListener
    void on(PaymentFailed event) {
        log.info("Rejecting order {} after payment failure", event.orderId());
        orders.reject(event.orderId(), "payment failed: " + event.reason());
    }

    @ApplicationModuleListener
    void on(PaymentSettled event) {
        log.info("Confirming order {}", event.orderId());
        orders.confirm(event.orderId());
    }
}
