package com.gkcontas.modulith.order;

import java.math.BigDecimal;

public record PlaceOrder(String sku, int quantity, BigDecimal amount, String customerEmail) {
}
