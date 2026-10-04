package com.gkcontas.modulith.notification.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findByOrderIdOrderByRecordedAtAsc(UUID orderId);
}
