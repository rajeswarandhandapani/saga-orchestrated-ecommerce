package com.rajeswaran.ecommerce.order.infrastructure.catalog;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.math.BigDecimal;
import java.util.List;

/**
 * Declarative HTTP client for Inventory's Open Host Service. The DTO mirrors Inventory's JSON, not our domain.
 */
@HttpExchange("/internal/products")
interface InventoryApi {

    record PriceQuoteDto(String sku, String name, BigDecimal unitPrice, String currency) {
    }

    @GetExchange
    List<PriceQuoteDto> quotes(@RequestParam("skus") List<String> skus);
}
