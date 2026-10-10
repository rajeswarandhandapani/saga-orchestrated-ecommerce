package com.rajeswaran.ecommerce.order.domain;

import com.rajeswaran.ecommerce.platform.domain.DomainException;

/**
 * Value object. The unit price is a snapshot taken when the order was placed - later catalogue price
 * changes in the Inventory context must not change what this customer agreed to pay.
 */
public record OrderLine(String sku, String productName, int quantity, Money unitPrice) {
    public OrderLine {
        if (quantity <= 0 || quantity > 100) {
            throw new DomainException("Quantity must be between 1 and 100");
        }
    }

    public Money lineTotal() {
        return unitPrice.times(quantity);
    }
}
