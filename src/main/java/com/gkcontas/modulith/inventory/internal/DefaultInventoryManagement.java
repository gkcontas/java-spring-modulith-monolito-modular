package com.gkcontas.modulith.inventory.internal;

import com.gkcontas.modulith.inventory.InventoryManagement;
import com.gkcontas.modulith.inventory.StockLevel;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DefaultInventoryManagement implements InventoryManagement {

    private final StockRepository repository;

    DefaultInventoryManagement(StockRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockLevel> levels() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(StockItemEntity::getSku))
                .map(item -> new StockLevel(item.getSku(), item.getAvailable(), item.getReserved()))
                .toList();
    }
}
