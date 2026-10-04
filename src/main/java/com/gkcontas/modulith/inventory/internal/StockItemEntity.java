package com.gkcontas.modulith.inventory.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockItemEntity {

    @Id
    @Column(length = 40)
    private String sku;

    @Column(nullable = false)
    private int available;

    @Column(nullable = false)
    private int reserved;
}
