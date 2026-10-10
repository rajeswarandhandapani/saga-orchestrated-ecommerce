package com.rajeswaran.ecommerce.inventory.infrastructure.persistence;

import com.rajeswaran.ecommerce.inventory.domain.Sku;
import com.rajeswaran.ecommerce.inventory.domain.StockItem;
import com.rajeswaran.ecommerce.inventory.domain.StockItems;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Saves copy state onto the managed entity loaded earlier in the same transaction,
 * so the entity's @Version still detects concurrent modification (optimistic locking).
 */
@Component
class JpaStockItems implements StockItems {

    private final StockItemJpaRepository stock;

    JpaStockItems(StockItemJpaRepository stock) {
        this.stock = stock;
    }

    @Override
    public Map<Sku, StockItem> findBySkus(Collection<Sku> skus) {
        return stock.findAllById(skus.stream().map(Sku::value).toList()).stream()
                .map(JpaStockItems::toDomain)
                .collect(Collectors.toMap(StockItem::id, Function.identity()));
    }

    @Override
    public Optional<StockItem> findBySku(Sku sku) {
        return stock.findById(sku.value()).map(JpaStockItems::toDomain);
    }

    @Override
    public List<StockItem> findAll() {
        return stock.findAll().stream().map(JpaStockItems::toDomain)
                .sorted(Comparator.comparing(s -> s.id().value())).toList();
    }

    @Override
    public void saveAll(Collection<StockItem> items) {
        for (StockItem item : items) {
            StockItemEntity e = stock.findById(item.id().value()).orElseThrow();
            e.onHand = item.onHand();
            e.reserved = item.reserved();
        }
    }

    private static StockItem toDomain(StockItemEntity e) {
        return new StockItem(new Sku(e.sku), e.onHand, e.reserved);
    }
}
