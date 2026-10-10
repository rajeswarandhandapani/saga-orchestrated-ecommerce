package com.rajeswaran.ecommerce.inventory.domain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Repository port for the StockItem aggregate. */
public interface StockItems {
    Map<Sku, StockItem> findBySkus(Collection<Sku> skus);

    Optional<StockItem> findBySku(Sku sku);

    List<StockItem> findAll();

    void saveAll(Collection<StockItem> items);
}
