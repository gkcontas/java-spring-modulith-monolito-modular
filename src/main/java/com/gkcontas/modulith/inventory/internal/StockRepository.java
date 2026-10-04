package com.gkcontas.modulith.inventory.internal;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

interface StockRepository extends JpaRepository<StockItemEntity, String> {

    /**
     * Pessimistic lock on the row being reserved.
     *
     * <p>Two orders for the same SKU arriving together would otherwise both read the same
     * availability and both reserve it. The listener is asynchronous, so "together" is the
     * normal case rather than the rare one.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StockItemEntity> findBySku(String sku);
}
