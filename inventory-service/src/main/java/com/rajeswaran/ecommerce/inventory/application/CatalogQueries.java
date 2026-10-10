package com.rajeswaran.ecommerce.inventory.application;

import com.rajeswaran.ecommerce.inventory.domain.*;
import com.rajeswaran.ecommerce.platform.domain.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Read side + admin operations of the Inventory context. */
@Service
@Transactional(readOnly = true)
public class CatalogQueries {

    public record ProductView(String sku, String name, String description, BigDecimal price, String currency,
                              int available) {
    }

    public record StockView(String sku, String name, int onHand, int reserved, int available) {
    }

    private final ProductCatalog catalog;
    private final StockItems stockItems;

    public CatalogQueries(ProductCatalog catalog, StockItems stockItems) {
        this.catalog = catalog;
        this.stockItems = stockItems;
    }

    public List<ProductView> products() {
        Map<Sku, StockItem> stock = stockItems.findBySkus(catalog.findAll().stream().map(Product::sku).toList());
        return catalog.findAll().stream().map(p -> view(p, stock.get(p.sku()))).toList();
    }

    public ProductView product(String sku) {
        Product product = catalog.findBySku(new Sku(sku)).orElseThrow(() -> new NotFoundException("No product " + sku));
        return view(product, stockItems.findBySku(product.sku()).orElse(null));
    }

    /** Used by the Ordering context (via /internal) to snapshot authoritative prices. */
    public List<Product> productsBySku(Collection<String> skus) {
        return catalog.findBySkus(skus.stream().map(Sku::new).toList());
    }

    public List<StockView> stock() {
        Map<Sku, String> names = catalog.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Product::sku, Product::name));
        return stockItems.findAll().stream()
                .map(s -> new StockView(s.id().value(), names.getOrDefault(s.id(), "?"), s.onHand(), s.reserved(),
                        s.available()))
                .toList();
    }

    @Transactional
    public StockView restock(String sku, int quantity) {
        StockItem item = stockItems.findBySku(new Sku(sku)).orElseThrow(() -> new NotFoundException("No stock item " + sku));
        item.restock(quantity);
        stockItems.saveAll(List.of(item));
        return new StockView(sku, catalog.findBySku(item.id()).map(Product::name).orElse("?"), item.onHand(),
                item.reserved(), item.available());
    }

    private static ProductView view(Product p, StockItem s) {
        return new ProductView(p.sku().value(), p.name(), p.description(), p.price().amount(), p.price().currency(),
                s == null ? 0 : s.available());
    }
}
