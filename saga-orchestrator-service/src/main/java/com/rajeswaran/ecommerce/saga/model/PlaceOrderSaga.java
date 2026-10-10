package com.rajeswaran.ecommerce.saga.model;

import com.rajeswaran.ecommerce.contracts.Command;
import com.rajeswaran.ecommerce.contracts.Event;
import com.rajeswaran.ecommerce.contracts.inventory.*;
import com.rajeswaran.ecommerce.contracts.order.*;
import com.rajeswaran.ecommerce.contracts.payment.*;

import java.util.List;

import static com.rajeswaran.ecommerce.saga.model.SagaState.*;

/**
 * The PlaceOrder saga as an explicit state-transition table. Pure logic: given the current saga state and an
 * incoming event, update the state and return the commands to send. No Spring, no Kafka - easy to unit test.
 *
 * <pre>
 * OrderPlaced ──────────────► AWAITING_RESERVATION  ⇒ ReserveInventory
 * InventoryReserved ────────► AWAITING_PAYMENT      ⇒ AuthorizePayment
 * InventoryRejected ────────► REJECTED              ⇒ RejectOrder
 * PaymentAuthorized ────────► COMPLETED             ⇒ CommitReservation, ConfirmOrder
 * PaymentDeclined ──────────► AWAITING_RELEASE      ⇒ ReleaseInventory        (compensation)
 * InventoryReleased ────────► COMPENSATED           ⇒ RejectOrder
 *
 * Abort (customer cancel / timeout) is a flag checked at the next reply:
 * InventoryReserved + abort ► AWAITING_RELEASE      ⇒ ReleaseInventory
 * PaymentAuthorized + abort ► AWAITING_REFUND       ⇒ RefundPayment
 * PaymentRefunded ──────────► AWAITING_RELEASE      ⇒ ReleaseInventory
 * InventoryReleased + abort ► CANCELLED             ⇒ CancelOrder
 * </pre>
 */
public final class PlaceOrderSaga {

    private PlaceOrderSaga() {
    }

    public record Started(SagaInstance saga, List<Command> commands) {
    }

    public static Started start(OrderPlaced event) {
        var saga = new SagaInstance(event.orderId(), event.customerId(), event.lines(), event.total(), event.currency());
        saga.record(SagaStep.Kind.RECEIVED, "OrderPlaced", "%s %s".formatted(event.total(), event.currency()), false);
        return new Started(saga, new Step(saga).go(AWAITING_RESERVATION, new ReserveInventory(event.orderId(), saga.lineItems())));
    }

    /** @return the commands to publish (possibly none). */
    public static List<Command> on(SagaInstance saga, Event event) {
        String name = event.getClass().getSimpleName();
        if (event instanceof OrderCancellationRequested) {
            return abort(saga, "Cancelled by customer");
        }
        if (saga.state().isFinal() || isInformational(event)) {
            saga.record(SagaStep.Kind.RECEIVED, name, "no action", false);
            return List.of();
        }
        var orderId = saga.orderId();
        return switch (saga.state()) {
            case AWAITING_RESERVATION -> switch (event) {
                case InventoryReserved e when saga.abortRequested() ->
                        received(saga, name, null).compensate(AWAITING_RELEASE, new ReleaseInventory(orderId, saga.abortReason()));
                case InventoryReserved e ->
                        received(saga, name, null).go(AWAITING_PAYMENT,
                                new AuthorizePayment(orderId, saga.customerId(), saga.total(), saga.currency()));
                case InventoryRejected e -> {
                    received(saga, name, e.reason());
                    saga.fail(e.reason());
                    yield saga.abortRequested()
                            ? new Step(saga).go(CANCELLED, new CancelOrder(orderId, saga.abortReason()))
                            : new Step(saga).go(REJECTED, new RejectOrder(orderId, e.reason()));
                }
                default -> unexpected(saga, name);
            };
            case AWAITING_PAYMENT -> switch (event) {
                case PaymentAuthorized e when saga.abortRequested() ->
                        received(saga, name, null).compensate(AWAITING_REFUND, new RefundPayment(orderId, saga.abortReason()));
                case PaymentAuthorized e ->
                        received(saga, name, null).go(COMPLETED, new CommitReservation(orderId), new ConfirmOrder(orderId));
                case PaymentDeclined e -> {
                    received(saga, name, e.reason());
                    saga.fail(e.reason());
                    yield new Step(saga).compensate(AWAITING_RELEASE, new ReleaseInventory(orderId, e.reason()));
                }
                default -> unexpected(saga, name);
            };
            case AWAITING_REFUND -> switch (event) {
                case PaymentRefunded e -> received(saga, name, null)
                        .compensate(AWAITING_RELEASE, new ReleaseInventory(orderId, saga.abortReason()));
                default -> unexpected(saga, name);
            };
            case AWAITING_RELEASE -> switch (event) {
                case InventoryReleased e when saga.abortRequested() ->
                        received(saga, name, null).go(CANCELLED, new CancelOrder(orderId, saga.abortReason()));
                case InventoryReleased e ->
                        received(saga, name, null).go(COMPENSATED, new RejectOrder(orderId, saga.failureReason()));
                default -> unexpected(saga, name);
            };
            default -> unexpected(saga, name);
        };
    }

    /** Customer cancellation or timeout: remembered and acted on at the next reply. */
    public static List<Command> abort(SagaInstance saga, String reason) {
        if (saga.state().isFinal()) {
            saga.record(SagaStep.Kind.NOTE, "Abort ignored", reason + " - saga already " + saga.state(), false);
        } else if (!saga.abortRequested()) {
            saga.requestAbort(reason);
            saga.record(SagaStep.Kind.NOTE, "Abort requested", reason + " - will unwind at next reply", true);
        }
        return List.of();
    }

    /** Events other contexts publish for anyone interested; the saga only logs them. */
    private static boolean isInformational(Event event) {
        return event instanceof OrderConfirmed || event instanceof OrderRejected || event instanceof OrderCancelled
                || event instanceof ReservationCommitted || event instanceof OrderPlaced;
    }

    private static Step received(SagaInstance saga, String name, String detail) {
        saga.record(SagaStep.Kind.RECEIVED, name, detail, false);
        return new Step(saga);
    }

    private static List<Command> unexpected(SagaInstance saga, String name) {
        saga.record(SagaStep.Kind.NOTE, "Ignored " + name, "unexpected in state " + saga.state(), false);
        return List.of();
    }

    private static Step send(SagaInstance saga, boolean compensation, Command... commands) {
        for (Command c : commands) {
            saga.record(SagaStep.Kind.SENT, c.getClass().getSimpleName(), null, compensation);
        }
        return new Step(saga, List.of(commands));
    }

    private record Step(SagaInstance saga, List<Command> commands) {
        Step(SagaInstance saga) {
            this(saga, List.of());
        }

        List<Command> go(SagaState next, Command... commands) {
            saga.moveTo(next);
            return send(saga, false, commands).commands;
        }

        List<Command> compensate(SagaState next, Command... commands) {
            saga.moveTo(next);
            return send(saga, true, commands).commands;
        }
    }
}
