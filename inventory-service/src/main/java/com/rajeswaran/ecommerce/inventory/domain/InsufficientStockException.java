package com.rajeswaran.ecommerce.inventory.domain;

import com.rajeswaran.ecommerce.platform.domain.DomainException;

/** Expected business outcome (not a bug): the saga turns it into InventoryRejected. */
public class InsufficientStockException extends DomainException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
