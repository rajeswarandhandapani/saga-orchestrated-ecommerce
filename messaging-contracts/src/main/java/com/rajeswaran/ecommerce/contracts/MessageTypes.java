package com.rajeswaran.ecommerce.contracts;

import com.rajeswaran.ecommerce.contracts.inventory.*;
import com.rajeswaran.ecommerce.contracts.order.*;
import com.rajeswaran.ecommerce.contracts.payment.*;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Registry of message type names (sent in the {@code messageType} Kafka header) to record classes.
 * Using a stable logical name instead of the Java class name keeps the wire format independent of packages.
 */
public final class MessageTypes {

    private static final Map<String, Class<? extends Message>> BY_NAME = Stream.<Class<? extends Message>>of(
                    // Ordering
                    OrderPlaced.class, OrderCancellationRequested.class, OrderConfirmed.class, OrderRejected.class,
                    OrderCancelled.class, ConfirmOrder.class, RejectOrder.class, CancelOrder.class,
                    // Inventory
                    ReserveInventory.class, CommitReservation.class, ReleaseInventory.class,
                    InventoryReserved.class, InventoryRejected.class, ReservationCommitted.class, InventoryReleased.class,
                    // Payments
                    AuthorizePayment.class, RefundPayment.class,
                    PaymentAuthorized.class, PaymentDeclined.class, PaymentRefunded.class)
            .collect(Collectors.toUnmodifiableMap(Class::getSimpleName, Function.identity()));

    private MessageTypes() {
    }

    public static String nameOf(Message message) {
        return message.getClass().getSimpleName();
    }

    public static Optional<Class<? extends Message>> classOf(String name) {
        return Optional.ofNullable(BY_NAME.get(name));
    }
}
