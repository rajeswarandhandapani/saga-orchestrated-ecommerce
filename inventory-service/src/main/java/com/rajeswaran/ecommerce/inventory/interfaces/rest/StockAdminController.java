package com.rajeswaran.ecommerce.inventory.interfaces.rest;

import com.rajeswaran.ecommerce.inventory.application.CatalogQueries;
import com.rajeswaran.ecommerce.inventory.application.CatalogQueries.StockView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@PreAuthorize("hasRole('ADMIN')")
class StockAdminController {

    record RestockRequest(@Positive int quantity) {
    }

    private final CatalogQueries queries;

    StockAdminController(CatalogQueries queries) {
        this.queries = queries;
    }

    @GetMapping
    List<StockView> stock() {
        return queries.stock();
    }

    @PostMapping("/{sku}/restock")
    StockView restock(@PathVariable String sku, @Valid @RequestBody RestockRequest request) {
        return queries.restock(sku, request.quantity());
    }
}
