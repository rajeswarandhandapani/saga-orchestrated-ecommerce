package com.rajeswaran.ecommerce.inventory.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** JPA representation of StockItem. @Version gives optimistic locking against concurrent reservations. */
@Entity
@Table(name = "stock_item")
class StockItemEntity {
    @Id
    String sku;
    int onHand;
    int reserved;
    @Version
    long version;
}
