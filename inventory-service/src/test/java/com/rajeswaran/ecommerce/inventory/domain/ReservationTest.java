package com.rajeswaran.ecommerce.inventory.domain;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {

    private final Sku keyboard = new Sku("KB-001");
    private final Sku mouse = new Sku("MS-001");

    private Map<Sku, StockItem> stock(int keyboards, int mice) {
        var map = new HashMap<Sku, StockItem>();
        map.put(keyboard, new StockItem(keyboard, keyboards, 0));
        map.put(mouse, new StockItem(mouse, mice, 0));
        return map;
    }

    @Test
    void reservesAllLinesAndRaisesEvent() {
        var stock = stock(5, 5);
        var reservation = Reservation.place(UUID.randomUUID(),
                List.of(new ReservationLine(keyboard, 2), new ReservationLine(mouse, 1)), stock);

        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(stock.get(keyboard).available()).isEqualTo(3);
        assertThat(stock.get(mouse).available()).isEqualTo(4);
        assertThat(reservation.pullDomainEvents()).hasSize(1).first().isInstanceOf(ReservationEvents.StockReserved.class);
    }

    @Test
    void isAllOrNothing() {
        var stock = stock(5, 1);
        assertThatThrownBy(() -> Reservation.place(UUID.randomUUID(),
                List.of(new ReservationLine(keyboard, 2), new ReservationLine(mouse, 2)), stock))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(stock.get(keyboard).available()).as("first line must not be reserved").isEqualTo(5);
    }

    @Test
    void releaseReturnsStockAndIsIdempotent() {
        var stock = stock(5, 5);
        var reservation = Reservation.place(UUID.randomUUID(), List.of(new ReservationLine(keyboard, 3)), stock);
        reservation.release(stock);
        reservation.release(stock);
        assertThat(stock.get(keyboard).available()).isEqualTo(5);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    void commitDeductsOnHand() {
        var stock = stock(5, 5);
        var reservation = Reservation.place(UUID.randomUUID(), List.of(new ReservationLine(keyboard, 3)), stock);
        reservation.commit(stock);
        assertThat(stock.get(keyboard).onHand()).isEqualTo(2);
        assertThat(stock.get(keyboard).reserved()).isZero();
        assertThatThrownBy(() -> reservation.release(stock)).hasMessageContaining("committed");
    }
}
