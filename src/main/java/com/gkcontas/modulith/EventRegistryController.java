package com.gkcontas.modulith;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The delivery registry, exposed so the mechanism can be seen rather than trusted.
 *
 * <p>Every {@code @ApplicationModuleListener} delivery is written to
 * {@code event_publication} before the listener runs, and the row is completed when it
 * returns. A listener that throws leaves its row with a null completion date — one row per
 * listener, so a failure in {@code payment} does not hide the fact that
 * {@code notification} handled the same event successfully.
 *
 * <p>That is the piece people are usually surprised by: asynchronous listeners inside a
 * monolith normally lose the event when they fail, exactly like a message consumer without
 * a dead-letter queue. The registry is what makes the in-process version recoverable, and
 * it is also what lets pending work survive a restart of the application.
 *
 * <p>This class sits in the application's root package, which belongs to no module: it is
 * infrastructure for all of them and business logic for none.
 */
@RestController
@RequestMapping("/event-registry")
class EventRegistryController {

    private final JdbcTemplate jdbc;
    private final IncompleteEventPublications incomplete;

    EventRegistryController(JdbcTemplate jdbc, IncompleteEventPublications incomplete) {
        this.jdbc = jdbc;
        this.incomplete = incomplete;
    }

    record PublicationView(UUID id, String listenerId, String eventType, Instant publicationDate,
                           Instant completionDate) {
    }

    /** Deliveries still pending: written, not yet acknowledged by their listener. */
    @GetMapping("/incomplete")
    List<PublicationView> incomplete() {
        return jdbc.query("""
                SELECT id, listener_id, event_type, publication_date, completion_date
                  FROM event_publication
                 WHERE completion_date IS NULL
                 ORDER BY publication_date
                """, EventRegistryController::toView);
    }

    @GetMapping
    List<PublicationView> all() {
        return jdbc.query("""
                SELECT id, listener_id, event_type, publication_date, completion_date
                  FROM event_publication
                 ORDER BY publication_date DESC
                 LIMIT 100
                """, EventRegistryController::toView);
    }

    /**
     * Replays every pending delivery.
     *
     * <p>Resubmission calls the listener again with the same event, so the listener has to
     * be idempotent — the same requirement a message consumer has. Here the payment
     * listener writes by primary key, so a repeat is a no-op rather than a double charge.
     */
    @PostMapping("/resubmit")
    ResubmitResult resubmit() {
        List<PublicationView> pending = incomplete();
        incomplete.resubmitIncompletePublications(publication -> true);
        // The count is taken before the call because resubmission is asynchronous, like
        // the original delivery: reading the table again right after would still show the
        // rows, now being retried. Check GET /event-registry/incomplete a moment later.
        return new ResubmitResult(pending.size(),
                pending.stream().map(PublicationView::listenerId).toList());
    }

    record ResubmitResult(int resubmitted, List<String> listeners) {
    }

    private static PublicationView toView(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new PublicationView(
                rs.getObject("id", UUID.class),
                rs.getString("listener_id"),
                rs.getString("event_type"),
                rs.getTimestamp("publication_date").toInstant(),
                rs.getTimestamp("completion_date") == null
                        ? null : rs.getTimestamp("completion_date").toInstant());
    }
}
