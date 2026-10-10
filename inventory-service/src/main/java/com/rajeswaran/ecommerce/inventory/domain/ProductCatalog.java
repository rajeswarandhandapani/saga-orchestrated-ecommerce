package com.rajeswaran.ecommerce.inventory.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Repository port (implemented in infrastructure). */
public interface ProductCatalog {
    List<Product> findAll();

    List<Product> findBySkus(Collection<Sku> skus);

    Optional<Product> findBySku(Sku sku);
}
