package com.rajeswaran.ecommerce.inventory.interfaces.rest;

import com.rajeswaran.ecommerce.inventory.application.CatalogQueries;
import com.rajeswaran.ecommerce.inventory.application.CatalogQueries.ProductView;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Public catalogue browsing. */
@RestController
@RequestMapping("/api/products")
class ProductController {

    private final CatalogQueries queries;

    ProductController(CatalogQueries queries) {
        this.queries = queries;
    }

    @GetMapping
    List<ProductView> list() {
        return queries.products();
    }

    @GetMapping("/{sku}")
    ProductView get(@PathVariable String sku) {
        return queries.product(sku);
    }
}
