package com.rajeswaran.ecommerce.inventory.domain;

import com.rajeswaran.ecommerce.inventory.domain.ReservationEvents.ReservationCommitted;
import com.rajeswaran.ecommerce.inventory.domain.ReservationEvents.ReservationReleased;
import com.rajeswaran.ecommerce.inventory.domain.ReservationEvents.StockReserved;
import com.rajeswaran.ecommerce.platform.domain.AggregateRoot;
import com.rajeswaran.ecommerce.platform.domain.DomainException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Aggregate: units held for one order. The Inventory context's view of an order is just this -
 * it knows nothing about customers, prices or payment.
 * <pre>RESERVED ─commit→ COMMITTED
 * RESERVED ─release→ RELEASED</pre>
 */
public class Reservation extends AggregateRoot<UUID> {

    private final UUID orderId;
    private final List<ReservationLine> lines;
    private ReservationStatus status;

    public Reservation(UUID orderId, List<ReservationLine> lines, ReservationStatus status) {
        this.orderId = orderId;
        this.lines = List.copyOf(lines);
        this.status = status;
    }

    /**
     * Factory: reserves every line or none. All stock items involved must be supplied.
     * Touching several StockItem aggregates in one local transaction is a conscious trade-off -
     * they live in the same context and database, and the all-or-nothing rule is a business requirement.
     */
    public static Reservation place(UUID orderId, List<ReservationLine> lines, Map<Sku, StockItem> stock) {
        if (lines.isEmpty()) {
            throw new DomainException("Reservation needs at least one line");
        }
        for (ReservationLine line : lines) {
            StockItem item = stock.get(line.sku());
            if (item == null) {
                throw new InsufficientStockException("Unknown SKU " + line.sku());
            }
            if (!item.canReserve(line.quantity())) {
                throw new InsufficientStockException("Insufficient stock for %s: requested %d, available %d"
                        .formatted(line.sku(), line.quantity(), item.available()));
            }
        }
        lines.forEach(line -> stock.get(line.sku()).reserve(line.quantity()));
        var reservation = new Reservation(orderId, lines, ReservationStatus.RESERVED);
        reservation.raise(new StockReserved(orderId));
        return reservation;
    }

    public void commit(Map<Sku, StockItem> stock) {
        if (status == ReservationStatus.COMMITTED) {
            raise(new ReservationCommitted(orderId)); // idempotent: re-announce
            return;
        }
        if (status != ReservationStatus.RESERVED) {
            throw new DomainException("Cannot commit a " + status + " reservation");
        }
        lines.forEach(line -> stock.get(line.sku()).commit(line.quantity()));
        status = ReservationStatus.COMMITTED;
        raise(new ReservationCommitted(orderId));
    }

    public void release(Map<Sku, StockItem> stock) {
        if (status == ReservationStatus.RESERVED) {
            lines.forEach(line -> stock.get(line.sku()).release(line.quantity()));
            status = ReservationStatus.RELEASED;
        } else if (status == ReservationStatus.COMMITTED) {
            throw new DomainException("Cannot release a committed reservation");
        }
        raise(new ReservationReleased(orderId));
    }

    @Override
    public UUID id() {
        return orderId;
    }

    public List<ReservationLine> lines() {
        return lines;
    }

    public ReservationStatus status() {
        return status;
    }

    public List<Sku> skus() {
        return lines.stream().map(ReservationLine::sku).toList();
    }
}
