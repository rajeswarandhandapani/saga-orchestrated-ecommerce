package com.rajeswaran.ecommerce.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ProductJpaRepository extends JpaRepository<ProductEntity, String> {
}

interface StockItemJpaRepository extends JpaRepository<StockItemEntity, String> {
}

interface ReservationJpaRepository extends JpaRepository<ReservationEntity, UUID> {
}
