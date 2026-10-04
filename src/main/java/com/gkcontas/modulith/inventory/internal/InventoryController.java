package com.gkcontas.modulith.inventory.internal;

import com.gkcontas.modulith.inventory.InventoryManagement;
import com.gkcontas.modulith.inventory.StockLevel;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stock")
class InventoryController {

    private final InventoryManagement inventory;

    InventoryController(InventoryManagement inventory) {
        this.inventory = inventory;
    }

    @GetMapping
    List<StockLevel> levels() {
        return inventory.levels();
    }
}
