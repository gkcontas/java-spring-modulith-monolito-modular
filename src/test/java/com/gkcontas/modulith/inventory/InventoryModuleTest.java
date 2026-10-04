package com.gkcontas.modulith.inventory;

import com.gkcontas.modulith.order.OrderPlaced;
import com.gkcontas.modulith.support.TestPostgres;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * One module, bootstrapped alone.
 *
 * <p>{@code @ApplicationModuleTest} starts a context containing only this module's beans.
 * {@code order}, {@code payment}, {@code fulfillment} and {@code notification} are not
 * there at all — so if {@code inventory} had quietly grown a dependency on one of them,
 * this context would fail to start. It is the runtime counterpart of the static
 * verification: one proves the imports are clean, the other proves the wiring is.
 *
 * <p>The outgoing events are captured by a listener declared in the test itself, and
 * {@code Scenario} waits for that recorder to fill. Waiting on observable state rather
 * than on the framework's event collector keeps the assertion independent of when each
 * asynchronous delivery happens to land, which is what makes the three tests stable when
 * they share a context and a thread pool.
 */
@ApplicationModuleTest
class InventoryModuleTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        TestPostgres.registerOn(registry);
    }

    /**
     * A listener living in the test, not in the module.
     *
     * <p>It stands in for the modules that are absent here — {@code payment} and
     * {@code notification} would be the real consumers of these events. That it can be
     * added from the outside, with no change to {@code inventory}, is the same property
     * that lets a new module subscribe in production.
     */
    @TestConfiguration
    static class EventRecorderConfiguration {

        @Bean
        EventRecorder eventRecorder() {
            return new EventRecorder();
        }
    }

    static class EventRecorder {

        private final List<Object> received = new CopyOnWriteArrayList<>();

        @EventListener
        void on(StockReserved event) {
            received.add(event);
        }

        @EventListener
        void on(StockRejected event) {
            received.add(event);
        }

        <T> Optional<T> find(Class<T> type, UUID orderId) {
            return received.stream()
                    .filter(type::isInstance)
                    .map(type::cast)
                    .filter(event -> orderIdOf(event).equals(orderId))
                    .findFirst();
        }

        private static UUID orderIdOf(Object event) {
            return event instanceof StockReserved reserved
                    ? reserved.orderId()
                    : ((StockRejected) event).orderId();
        }
    }

    private static OrderPlaced orderFor(String sku, int quantity) {
        return new OrderPlaced(UUID.randomUUID(), sku, quantity, new BigDecimal("199.00"),
                "ana@example.com");
    }

    @Test
    void reservesStockAndAnnouncesIt(Scenario scenario, @Autowired EventRecorder recorder) {
        OrderPlaced event = orderFor("MOUSE-02", 3);

        scenario.publish(event)
                .andWaitAtMost(TIMEOUT)
                .andWaitForStateChange(() -> recorder.find(StockReserved.class, event.orderId()))
                .andVerify(reserved -> {
                    assertThat(reserved).isPresent();
                    assertThat(reserved.get().sku()).isEqualTo("MOUSE-02");
                    assertThat(reserved.get().quantity()).isEqualTo(3);
                    // The amount is carried forward so payment never has to ask order.
                    assertThat(reserved.get().amount()).isEqualByComparingTo("199.00");
                });
    }

    @Test
    void rejectsWhenThereIsNotEnoughStock(Scenario scenario, @Autowired EventRecorder recorder) {
        OrderPlaced event = orderFor("MONITOR-03", 99);

        scenario.publish(event)
                .andWaitAtMost(TIMEOUT)
                .andWaitForStateChange(() -> recorder.find(StockRejected.class, event.orderId()))
                .andVerify(rejected ->
                        assertThat(rejected).hasValueSatisfying(value ->
                                assertThat(value.reason()).contains("available")));
    }

    @Test
    void rejectsAnUnknownSku(Scenario scenario, @Autowired EventRecorder recorder) {
        OrderPlaced event = orderFor("DOES-NOT-EXIST", 1);

        scenario.publish(event)
                .andWaitAtMost(TIMEOUT)
                .andWaitForStateChange(() -> recorder.find(StockRejected.class, event.orderId()))
                .andVerify(rejected ->
                        assertThat(rejected).hasValueSatisfying(value ->
                                assertThat(value.reason()).isEqualTo("unknown sku")));
    }
}
