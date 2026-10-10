package com.rajeswaran.ecommerce.saga.infrastructure;

import com.rajeswaran.ecommerce.saga.model.SagaInstance;
import com.rajeswaran.ecommerce.saga.model.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SagaRepository extends JpaRepository<SagaInstance, UUID> {

    Optional<SagaInstance> findByOrderId(UUID orderId);

    List<SagaInstance> findTop100ByOrderByStartedAtDesc();

    List<SagaInstance> findByStateInAndAbortRequestedFalseAndUpdatedAtBefore(Collection<SagaState> states, Instant before);
}
