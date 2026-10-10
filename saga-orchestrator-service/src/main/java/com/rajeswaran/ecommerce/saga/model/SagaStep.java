package com.rajeswaran.ecommerce.saga.model;

import jakarta.persistence.Embeddable;

import java.time.Instant;

/** One entry in the saga's audit trail - shown as a timeline in the UI. */
@Embeddable
public class SagaStep {

    public enum Kind { RECEIVED, SENT, NOTE }

    private Instant at;
    private String kind;
    private String message;
    private String detail;
    private boolean compensation;

    protected SagaStep() {
    }

    public SagaStep(Kind kind, String message, String detail, boolean compensation) {
        this.at = Instant.now();
        this.kind = kind.name();
        this.message = message;
        this.detail = detail;
        this.compensation = compensation;
    }

    public Instant at() { return at; }
    public String kind() { return kind; }
    public String message() { return message; }
    public String detail() { return detail; }
    public boolean compensation() { return compensation; }
}
