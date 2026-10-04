package com.gkcontas.modulith.notification.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(nullable = false, length = 40)
    private String event;

    @Column(nullable = false, length = 300)
    private String message;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    NotificationEntity(UUID orderId, String event, String message, Instant recordedAt) {
        this(null, orderId, event, message, recordedAt);
    }
}
