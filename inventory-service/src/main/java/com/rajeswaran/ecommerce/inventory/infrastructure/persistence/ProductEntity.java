package com.rajeswaran.ecommerce.inventory.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "product")
class ProductEntity {
    @Id
    String sku;
    String name;
    String description;
    BigDecimal price;
    String currency;
}
