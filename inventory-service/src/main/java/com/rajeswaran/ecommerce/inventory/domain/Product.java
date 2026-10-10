package com.rajeswaran.ecommerce.inventory.domain;

/**
 * Catalogue entry. In Inventory a "product" is a SKU with a name and list price;
 * the Ordering context has no Product at all - it only snapshots sku + price on an order line.
 */
public record Product(Sku sku, String name, String description, Price price) {
}
