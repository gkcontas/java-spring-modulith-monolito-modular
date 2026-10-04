package com.gkcontas.modulith.inventory;

import java.util.UUID;

public record StockRejected(UUID orderId, String sku, String reason) {
}
