package com.rajeswaran.ecommerce.inventory.interfaces.rest;

import com.rajeswaran.ecommerce.inventory.application.CatalogQueries;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Open Host Service for other contexts (synchronous, service-to-service; not routed by the gateway).
 * The response is a purpose-built DTO, not the domain Product, so Inventory can evolve its model freely.
 */
@RestController
@RequestMapping("/internal/products")
class InternalProductController {

    record PriceQuote(String sku, String name, BigDecimal unitPrice, String currency) {
    }

    private final CatalogQueries queries;

    InternalProductController(CatalogQueries queries) {
        this.queries = queries;
    }

    @GetMapping
    List<PriceQuote> quotes(@RequestParam List<String> skus) {
        return queries.productsBySku(skus).stream()
                .map(p -> new PriceQuote(p.sku().value(), p.name(), p.price().amount(), p.price().currency()))
                .toList();
    }
}
