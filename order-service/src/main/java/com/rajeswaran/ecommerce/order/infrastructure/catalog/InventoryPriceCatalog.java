package com.rajeswaran.ecommerce.order.infrastructure.catalog;

import com.rajeswaran.ecommerce.order.domain.Money;
import com.rajeswaran.ecommerce.order.domain.PriceCatalog;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Anti-Corruption Layer: translates Inventory's PriceQuote DTO into Ordering's PricedItem / Money.
 * If Inventory renames fields or changes its model, only this class changes.
 */
@Component
class InventoryPriceCatalog implements PriceCatalog {

    private final InventoryApi inventory;

    InventoryPriceCatalog(InventoryApi inventory) {
        this.inventory = inventory;
    }

    @Override
    public Map<String, PricedItem> priceOf(Collection<String> skus) {
        try {
            return inventory.quotes(List.copyOf(skus)).stream()
                    .map(q -> new PricedItem(q.sku(), q.name(), new Money(q.unitPrice(), q.currency())))
                    .collect(Collectors.toMap(PricedItem::sku, Function.identity()));
        } catch (RestClientException e) {
            throw new CatalogUnavailableException("Inventory service unavailable: " + e.getMessage(), e);
        }
    }
}
