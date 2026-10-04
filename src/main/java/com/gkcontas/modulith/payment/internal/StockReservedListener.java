package com.gkcontas.modulith.payment.internal;

import com.gkcontas.modulith.inventory.StockReserved;
import com.gkcontas.modulith.payment.PaymentFailed;
import com.gkcontas.modulith.payment.PaymentSettled;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Charges only after stock is reserved, and distinguishes two kinds of failure.
 *
 * <p>The distinction is the point of this class:
 *
 * <ul>
 *   <li>A <b>business failure</b> — the amount is above the limit — is a normal outcome.
 *       It is recorded, published as {@code PaymentFailed}, and the flow continues to its
 *       conclusion. The listener returns normally.
 *   <li>A <b>technical failure</b> — the simulated gateway being unreachable — is not an
 *       outcome at all. The listener throws, its transaction rolls back, and the delivery
 *       stays recorded as incomplete in the publication registry, where it can be
 *       inspected and resubmitted.
 * </ul>
 *
 * <p>Confusing the two is a common and expensive mistake: swallowing a technical failure
 * turns a retryable problem into a permanently stuck order, and throwing on a business
 * failure fills the registry with entries that will never succeed no matter how often they
 * are retried.
 */
@Component
class StockReservedListener {

    private static final Logger log = LoggerFactory.getLogger(StockReservedListener.class);

    /** Above this, the simulated issuer declines. A business outcome. */
    private static final BigDecimal LIMIT = new BigDecimal("10000.00");

    /** SKUs starting with this make the simulated gateway unreachable. A technical failure. */
    static final String UNREACHABLE_GATEWAY_PREFIX = "BOOM";

    /**
     * Orders whose first delivery already failed.
     *
     * <p>The simulated outage is transient on purpose: the first attempt throws, a
     * resubmission goes through. A permanent failure would show the publication staying
     * pending, which is half the story — the half that matters is that replaying it
     * finishes the order that was stuck.
     */
    private final Set<UUID> alreadyAttempted = ConcurrentHashMap.newKeySet();

    private final PaymentRepository repository;
    private final ApplicationEventPublisher events;

    StockReservedListener(PaymentRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    @ApplicationModuleListener
    void on(StockReserved event) {
        if (event.sku().startsWith(UNREACHABLE_GATEWAY_PREFIX)
                && alreadyAttempted.add(event.orderId())) {
            throw new IllegalStateException(
                    "Payment gateway unreachable for order " + event.orderId());
        }

        boolean approved = event.amount().compareTo(LIMIT) <= 0;
        String outcome = approved ? "SETTLED" : "DECLINED";

        repository.save(new PaymentEntity(event.orderId(), event.amount(), outcome, Instant.now()));
        log.info("Payment {} for order {}", outcome, event.orderId());

        if (approved) {
            events.publishEvent(new PaymentSettled(event.orderId(), event.amount()));
        } else {
            events.publishEvent(new PaymentFailed(event.orderId(),
                    "amount above the %s limit".formatted(LIMIT)));
        }
    }
}
