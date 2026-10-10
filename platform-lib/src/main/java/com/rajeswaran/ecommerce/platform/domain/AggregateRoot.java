package com.rajeswaran.ecommerce.platform.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for aggregate roots: the single entry point through which an aggregate's invariants are enforced.
 * Collects domain events raised by behaviour methods until the application service pulls them.
 * Deliberately free of Spring and JPA so domain code stays plain Java.
 */
public abstract class AggregateRoot<ID> {

    private final transient List<DomainEvent> domainEvents = new ArrayList<>();

    public abstract ID id();

    protected void raise(DomainEvent event) {
        domainEvents.add(event);
    }

    /** Returns and clears the pending events. */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }
}
