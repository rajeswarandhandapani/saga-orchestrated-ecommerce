package com.rajeswaran.ecommerce.inventory.application;

import com.rajeswaran.ecommerce.contracts.Topics;
import com.rajeswaran.ecommerce.contracts.inventory.*;
import com.rajeswaran.ecommerce.inventory.domain.*;
import com.rajeswaran.ecommerce.platform.domain.DomainEvent;
import com.rajeswaran.ecommerce.platform.messaging.OutboxPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Application service for the saga's inventory steps. Each method: load aggregates → call domain behaviour →
 * save → translate domain events into integration events on the outbox. All in one local transaction.
 * Every handler is idempotent at the business level too (a repeated command re-announces the current outcome).
 */
@Service
@Transactional
public class InventoryCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(InventoryCommandHandler.class);

    private final StockItems stockItems;
    private final Reservations reservations;
    private final OutboxPublisher outbox;

    public InventoryCommandHandler(StockItems stockItems, Reservations reservations, OutboxPublisher outbox) {
        this.stockItems = stockItems;
        this.reservations = reservations;
        this.outbox = outbox;
    }

    public void handle(InventoryCommand command) {
        switch (command) {
            case ReserveInventory c -> reserve(c);
            case CommitReservation c -> commit(c.orderId());
            case ReleaseInventory c -> release(c.orderId(), c.reason());
        }
    }

    void reserve(ReserveInventory command) {
        var existing = reservations.findByOrderId(command.orderId());
        if (existing.isPresent()) {
            log.info("Reservation for order {} already {}", command.orderId(), existing.get().status());
            outbox.append(Topics.INVENTORY_EVENTS, existing.get().status() == ReservationStatus.RELEASED
                    ? new InventoryRejected(command.orderId(), "Reservation was already released")
                    : new InventoryReserved(command.orderId()));
            return;
        }
        List<ReservationLine> lines = command.lines().stream()
                .map(l -> new ReservationLine(new Sku(l.sku()), l.quantity()))
                .toList();
        Map<Sku, StockItem> stock = stockItems.findBySkus(lines.stream().map(ReservationLine::sku).toList());
        try {
            Reservation reservation = Reservation.place(command.orderId(), lines, stock);
            stockItems.saveAll(stock.values());
            reservations.save(reservation);
            publish(reservation.pullDomainEvents());
        } catch (InsufficientStockException e) {
            log.info("Rejecting reservation for order {}: {}", command.orderId(), e.getMessage());
            outbox.append(Topics.INVENTORY_EVENTS, new InventoryRejected(command.orderId(), e.getMessage()));
        }
    }

    void commit(UUID orderId) {
        Reservation reservation = reservations.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("No reservation to commit for order " + orderId));
        Map<Sku, StockItem> stock = stockItems.findBySkus(reservation.skus());
        reservation.commit(stock);
        stockItems.saveAll(stock.values());
        reservations.save(reservation);
        publish(reservation.pullDomainEvents());
    }

    /** Compensation. Releasing an order that never reserved anything is a no-op that still replies. */
    void release(UUID orderId, String reason) {
        var found = reservations.findByOrderId(orderId);
        if (found.isEmpty()) {
            log.info("Nothing reserved for order {} - acknowledging release ({})", orderId, reason);
            outbox.append(Topics.INVENTORY_EVENTS, new InventoryReleased(orderId));
            return;
        }
        Reservation reservation = found.get();
        Map<Sku, StockItem> stock = stockItems.findBySkus(reservation.skus());
        reservation.release(stock);
        stockItems.saveAll(stock.values());
        reservations.save(reservation);
        publish(reservation.pullDomainEvents());
    }

    /** Domain event → integration event translation: the edge of the bounded context. */
    private void publish(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            switch (event) {
                case ReservationEvents.StockReserved e ->
                        outbox.append(Topics.INVENTORY_EVENTS, new InventoryReserved(e.orderId()));
                case ReservationEvents.ReservationCommitted e ->
                        outbox.append(Topics.INVENTORY_EVENTS, new ReservationCommitted(e.orderId()));
                case ReservationEvents.ReservationReleased e ->
                        outbox.append(Topics.INVENTORY_EVENTS, new InventoryReleased(e.orderId()));
                default -> log.debug("No integration event for {}", event);
            }
        }
    }
}
