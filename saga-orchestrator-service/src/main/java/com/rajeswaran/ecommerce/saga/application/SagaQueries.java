package com.rajeswaran.ecommerce.saga.application;

import com.rajeswaran.ecommerce.platform.domain.NotFoundException;
import com.rajeswaran.ecommerce.saga.infrastructure.SagaRepository;
import com.rajeswaran.ecommerce.saga.model.SagaInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SagaQueries {

    public record StepView(Instant at, String kind, String message, String detail, boolean compensation) {
    }

    public record SagaView(UUID sagaId, UUID orderId, String customerId, String state, BigDecimal total,
                           String currency, boolean abortRequested, String abortReason, String failureReason,
                           Instant startedAt, Instant updatedAt, List<StepView> steps) {
        static SagaView of(SagaInstance s, boolean withSteps) {
            return new SagaView(s.id(), s.orderId(), s.customerId(), s.state().name(), s.total(), s.currency(),
                    s.abortRequested(), s.abortReason(), s.failureReason(), s.startedAt(), s.updatedAt(),
                    withSteps ? s.steps().stream()
                            .map(st -> new StepView(st.at(), st.kind(), st.message(), st.detail(), st.compensation()))
                            .toList() : List.of());
        }
    }

    private final SagaRepository sagas;

    public SagaQueries(SagaRepository sagas) {
        this.sagas = sagas;
    }

    public List<SagaView> recent() {
        return sagas.findTop100ByOrderByStartedAtDesc().stream().map(s -> SagaView.of(s, false)).toList();
    }

    public SagaView forOrder(UUID orderId) {
        return sagas.findByOrderId(orderId).map(s -> SagaView.of(s, true))
                .orElseThrow(() -> new NotFoundException("No saga for order " + orderId));
    }
}
