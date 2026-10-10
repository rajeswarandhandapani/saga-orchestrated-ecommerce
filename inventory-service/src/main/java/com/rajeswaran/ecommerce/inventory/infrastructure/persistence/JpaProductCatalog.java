package com.rajeswaran.ecommerce.inventory.infrastructure.persistence;

import com.rajeswaran.ecommerce.inventory.domain.Price;
import com.rajeswaran.ecommerce.inventory.domain.Product;
import com.rajeswaran.ecommerce.inventory.domain.ProductCatalog;
import com.rajeswaran.ecommerce.inventory.domain.Sku;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Adapter implementing a domain repository port with Spring Data JPA.
 * Mapping between JPA entities and domain objects keeps persistence annotations out of the domain model.
 */
@Component
class JpaProductCatalog implements ProductCatalog {

    private final ProductJpaRepository products;

    JpaProductCatalog(ProductJpaRepository products) {
        this.products = products;
    }

    @Override
    public List<Product> findAll() {
        return products.findAll().stream().map(JpaProductCatalog::toDomain)
                .sorted(Comparator.comparing(p -> p.sku().value())).toList();
    }

    @Override
    public List<Product> findBySkus(Collection<Sku> skus) {
        return products.findAllById(skus.stream().map(Sku::value).toList()).stream()
                .map(JpaProductCatalog::toDomain).toList();
    }

    @Override
    public Optional<Product> findBySku(Sku sku) {
        return products.findById(sku.value()).map(JpaProductCatalog::toDomain);
    }

    private static Product toDomain(ProductEntity e) {
        return new Product(new Sku(e.sku), e.name, e.description, new Price(e.price, e.currency));
    }
}
