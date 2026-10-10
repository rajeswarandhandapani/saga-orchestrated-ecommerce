package com.rajeswaran.ecommerce.contracts;

import java.util.UUID;

/**
 * Marker for everything that travels over Kafka between bounded contexts.
 * <p>
 * These records are the <b>Published Language</b> of the system: the only types shared between services.
 * Each context translates them into its own domain model (and never exposes its aggregates on the wire).
 * Every message is about one order, and {@link #orderId()} is used as the Kafka key so all messages for an
 * order land on the same partition and are consumed in order.
 */
public sealed interface Message permits Command, Event {
    UUID orderId();
}
