package com.rajeswaran.ecommerce.order.infrastructure.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The synchronous dependency failed - the trade-off of sync calls: Ordering can't take orders without Inventory. */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class CatalogUnavailableException extends RuntimeException {
    public CatalogUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
