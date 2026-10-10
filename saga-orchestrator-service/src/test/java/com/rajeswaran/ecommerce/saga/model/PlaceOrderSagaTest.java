package com.rajeswaran.ecommerce.saga.model;

import com.rajeswaran.ecommerce.contracts.LineItem;
import com.rajeswaran.ecommerce.contracts.inventory.*;
import com.rajeswaran.ecommerce.contracts.order.*;
import com.rajeswaran.ecommerce.contracts.payment.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderSagaTest {

    private final UUID orderId = UUID.randomUUID();

    private SagaInstance started() {
        var started = PlaceOrderSaga.start(new OrderPlaced(orderId, "alice",
                List.of(new LineItem("KB-001", 1, new BigDecimal("89.00"))), new BigDecimal("89.00"), "USD"));
        assertThat(started.commands()).singleElement().isInstanceOf(ReserveInventory.class);
        return started.saga();
    }

    @Test
    void happyPath() {
        var saga = started();
        assertThat(PlaceOrderSaga.on(saga, new InventoryReserved(orderId)))
                .singleElement().isInstanceOf(AuthorizePayment.class);
        assertThat(PlaceOrderSaga.on(saga, new PaymentAuthorized(orderId, UUID.randomUUID())))
                .hasExactlyElementsOfTypes(CommitReservation.class, ConfirmOrder.class);
        assertThat(saga.state()).isEqualTo(SagaState.COMPLETED);
    }

    @Test
    void outOfStockRejectsWithoutCompensation() {
        var saga = started();
        assertThat(PlaceOrderSaga.on(saga, new InventoryRejected(orderId, "none left")))
                .singleElement().isEqualTo(new RejectOrder(orderId, "none left"));
        assertThat(saga.state()).isEqualTo(SagaState.REJECTED);
    }

    @Test
    void paymentDeclinedReleasesStockThenRejects() {
        var saga = started();
        PlaceOrderSaga.on(saga, new InventoryReserved(orderId));
        assertThat(PlaceOrderSaga.on(saga, new PaymentDeclined(orderId, "limit")))
                .singleElement().isInstanceOf(ReleaseInventory.class);
        assertThat(PlaceOrderSaga.on(saga, new InventoryReleased(orderId)))
                .singleElement().isEqualTo(new RejectOrder(orderId, "limit"));
        assertThat(saga.state()).isEqualTo(SagaState.COMPENSATED);
    }

    @Test
    void cancellationAfterPaymentRefundsThenReleasesThenCancels() {
        var saga = started();
        PlaceOrderSaga.on(saga, new InventoryReserved(orderId));
        assertThat(PlaceOrderSaga.on(saga, new OrderCancellationRequested(orderId))).isEmpty();
        assertThat(PlaceOrderSaga.on(saga, new PaymentAuthorized(orderId, UUID.randomUUID())))
                .singleElement().isInstanceOf(RefundPayment.class);
        assertThat(PlaceOrderSaga.on(saga, new PaymentRefunded(orderId)))
                .singleElement().isInstanceOf(ReleaseInventory.class);
        assertThat(PlaceOrderSaga.on(saga, new InventoryReleased(orderId)))
                .singleElement().isInstanceOf(CancelOrder.class);
        assertThat(saga.state()).isEqualTo(SagaState.CANCELLED);
    }

    @Test
    void lateCancellationOfCompletedSagaIsIgnored() {
        var saga = started();
        PlaceOrderSaga.on(saga, new InventoryReserved(orderId));
        PlaceOrderSaga.on(saga, new PaymentAuthorized(orderId, UUID.randomUUID()));
        assertThat(PlaceOrderSaga.on(saga, new OrderCancellationRequested(orderId))).isEmpty();
        assertThat(saga.state()).isEqualTo(SagaState.COMPLETED);
        assertThat(saga.abortRequested()).isFalse();
    }

    @Test
    void unexpectedEventsAreIgnored() {
        var saga = started();
        assertThat(PlaceOrderSaga.on(saga, new PaymentAuthorized(orderId, UUID.randomUUID()))).isEmpty();
        assertThat(saga.state()).isEqualTo(SagaState.AWAITING_RESERVATION);
    }
}
