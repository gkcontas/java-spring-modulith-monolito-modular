package com.gkcontas.modulith.order.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
}
