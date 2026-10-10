package com.rajeswaran.ecommerce.order.domain;

import com.rajeswaran.ecommerce.platform.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private final CustomerId alice = new CustomerId("alice");

    private Order newOrder() {
        return Order.place(alice, List.of(
                new OrderLine("KB-001", "Keyboard", 2, new Money(new BigDecimal("89.00"), "USD")),
                new OrderLine("CB-001", "Cable", 1, new Money(new BigDecimal("12.00"), "USD"))));
    }

    @Test
    void placingAnOrderComputesTotalAndRaisesEvent() {
        Order order = newOrder();
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.total().amount()).isEqualByComparingTo("190.00");
        assertThat(order.pullDomainEvents()).singleElement().isInstanceOf(OrderEvents.OrderPlaced.class);
    }

    @Test
    void orderNeedsLines() {
        assertThatThrownBy(() -> Order.place(alice, List.of())).isInstanceOf(DomainException.class);
    }

    @Test
    void confirmIsIdempotentButFinalStatesAreFinal() {
        Order order = newOrder();
        order.pullDomainEvents();
        order.confirm();
        order.confirm();
        assertThat(order.pullDomainEvents()).hasSize(1);
        assertThatThrownBy(() -> order.reject("late")).isInstanceOf(DomainException.class);
    }

    @Test
    void onlyBuyerCanRequestCancellationAndOnlyWhilePending() {
        Order order = newOrder();
        assertThatThrownBy(() -> order.requestCancellation(new CustomerId("mallory"))).isInstanceOf(DomainException.class);
        order.requestCancellation(alice);
        assertThat(order.cancellationRequested()).isTrue();
        order.cancel("customer request");
        assertThatThrownBy(() -> order.requestCancellation(alice)).hasMessageContaining("CANCELLED");
    }
}
