package com.rajeswaran.ecommerce.saga.model;

import com.rajeswaran.ecommerce.contracts.LineItem;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persistent state of one PlaceOrder saga (a process manager, not a business aggregate).
 * It keeps just enough data copied from {@code OrderPlaced} to issue later commands without calling back
 * into the Ordering context.
 */
@Entity
@Table(name = "saga_instance")
public class SagaInstance {

    @Id
    private UUID id;
    @Column(unique = true)
    private UUID orderId;
    private String customerId;
    private BigDecimal total;
    private String currency;
    @Enumerated(EnumType.STRING)
    private SagaState state;
    private boolean abortRequested;
    private String abortReason;
    private String failureReason;
    private Instant startedAt;
    private Instant updatedAt;
    @Version
    private long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "saga_line", joinColumns = @JoinColumn(name = "saga_id"))
    @OrderColumn(name = "line_no")
    private List<Line> lines = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "saga_step", joinColumns = @JoinColumn(name = "saga_id"))
    @OrderColumn(name = "step_no")
    private List<SagaStep> steps = new ArrayList<>();

    @Embeddable
    public static class Line {
        private String sku;
        private int quantity;

        protected Line() {
        }

        Line(String sku, int quantity) {
            this.sku = sku;
            this.quantity = quantity;
        }
    }

    protected SagaInstance() {
    }

    public SagaInstance(UUID orderId, String customerId, List<LineItem> lines, BigDecimal total, String currency) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.customerId = customerId;
        this.total = total;
        this.currency = currency;
        this.lines = new ArrayList<>(lines.stream().map(l -> new Line(l.sku(), l.quantity())).toList());
        this.startedAt = Instant.now();
        this.updatedAt = startedAt;
    }

    // ---- state changes used by PlaceOrderSaga
    void moveTo(SagaState next) {
        this.state = next;
        this.updatedAt = Instant.now();
    }

    void requestAbort(String reason) {
        this.abortRequested = true;
        this.abortReason = reason;
    }

    void fail(String reason) {
        this.failureReason = reason;
    }

    public void record(SagaStep.Kind kind, String message, String detail, boolean compensation) {
        steps.add(new SagaStep(kind, message, detail, compensation));
    }

    // ---- accessors
    public UUID id() { return id; }
    public UUID orderId() { return orderId; }
    public String customerId() { return customerId; }
    public BigDecimal total() { return total; }
    public String currency() { return currency; }
    public SagaState state() { return state; }
    public boolean abortRequested() { return abortRequested; }
    public String abortReason() { return abortReason; }
    public String failureReason() { return failureReason; }
    public Instant startedAt() { return startedAt; }
    public Instant updatedAt() { return updatedAt; }
    public List<SagaStep> steps() { return List.copyOf(steps); }

    public List<LineItem> lineItems() {
        return lines.stream().map(l -> new LineItem(l.sku, l.quantity, null)).toList();
    }
}
