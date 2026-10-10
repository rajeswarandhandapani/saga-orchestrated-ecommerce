package com.rajeswaran.ecommerce.platform.domain;

/**
 * Something meaningful that happened inside ONE bounded context, expressed in that context's language.
 * <p>
 * Domain events are internal. The application layer translates the ones other contexts care about into
 * integration events (records in {@code messaging-contracts}) - that translation is the context boundary.
 */
public interface DomainEvent {
}
