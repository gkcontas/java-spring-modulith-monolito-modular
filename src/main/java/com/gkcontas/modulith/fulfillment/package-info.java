/**
 * The process manager: it listens to the outcomes of the other modules and turns them into
 * a decision about the order.
 *
 * <p>It exists for a structural reason. If {@code order} itself listened to
 * {@code inventory}'s events, the two would depend on each other and the module
 * verification would report a cycle — correctly, because mutual dependency is exactly what
 * makes a module impossible to extract later.
 *
 * <p>Concentrating the coordination in a module of its own keeps the graph acyclic: it
 * knows everybody, and nobody knows it. In a larger system this is where a saga, with its
 * compensations and timeouts, would live.
 *
 * <p>{@code allowedDependencies} states the three it may use. Without it the module could
 * grow a dependency on a fourth and nothing would object; with it, the verification test
 * fails on the import.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Fulfillment",
        allowedDependencies = {"order", "inventory", "payment"})
package com.gkcontas.modulith.fulfillment;
