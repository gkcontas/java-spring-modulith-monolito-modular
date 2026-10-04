package com.gkcontas.modulith.inventory;

public record StockLevel(String sku, int available, int reserved) {
}
