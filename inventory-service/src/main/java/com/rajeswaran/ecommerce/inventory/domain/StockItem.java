package com.rajeswaran.ecommerce.inventory.domain;

import com.rajeswaran.ecommerce.platform.domain.AggregateRoot;
import com.rajeswaran.ecommerce.platform.domain.DomainException;

/**
 * Aggregate: how many units of one SKU are physically on hand and how many are promised to orders.
 * Invariant: 0 &lt;= reserved &lt;= onHand.
 */
public class StockItem extends AggregateRoot<Sku> {

    private final Sku sku;
    private int onHand;
    private int reserved;

    public StockItem(Sku sku, int onHand, int reserved) {
        if (onHand < 0 || reserved < 0 || reserved > onHand) {
            throw new DomainException("Invalid stock levels for " + sku);
        }
        this.sku = sku;
        this.onHand = onHand;
        this.reserved = reserved;
    }

    @Override
    public Sku id() {
        return sku;
    }

    public int onHand() {
        return onHand;
    }

    public int reserved() {
        return reserved;
    }

    public int available() {
        return onHand - reserved;
    }

    public boolean canReserve(int quantity) {
        return quantity > 0 && quantity <= available();
    }

    void reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new DomainException("Cannot reserve %d of %s, only %d available".formatted(quantity, sku, available()));
        }
        reserved += quantity;
    }

    /** Compensation: return a reservation to the available pool. */
    void release(int quantity) {
        reserved = Math.max(0, reserved - quantity);
    }

    /** Reserved units leave the warehouse: both counters go down. */
    void commit(int quantity) {
        if (quantity > reserved) {
            throw new DomainException("Cannot commit more than reserved for " + sku);
        }
        reserved -= quantity;
        onHand -= quantity;
    }

    public void restock(int quantity) {
        if (quantity <= 0) {
            throw new DomainException("Restock quantity must be positive");
        }
        onHand += quantity;
    }
}
