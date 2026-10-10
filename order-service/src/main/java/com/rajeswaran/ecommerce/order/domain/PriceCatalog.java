package com.rajeswaran.ecommerce.order.domain;

import java.util.Collection;
import java.util.Map;

/**
 * Port to another bounded context (Inventory), expressed in Ordering's language.
 * The implementation is an Anti-Corruption Layer that calls Inventory synchronously and translates its DTOs.
 */
public interface PriceCatalog {

    record PricedItem(String sku, String name, Money unitPrice) {
    }

    /** @return priced items keyed by SKU; unknown SKUs are simply absent. */
    Map<String, PricedItem> priceOf(Collection<String> skus);
}
