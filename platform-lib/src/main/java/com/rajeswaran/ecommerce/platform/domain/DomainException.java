package com.rajeswaran.ecommerce.platform.domain;

/** A business rule (invariant) was violated. Mapped to HTTP 409/422 by the web layer. */
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
