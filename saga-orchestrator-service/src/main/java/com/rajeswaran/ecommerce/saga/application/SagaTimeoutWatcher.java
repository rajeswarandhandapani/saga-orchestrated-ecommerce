package com.rajeswaran.ecommerce.saga.application;

import com.rajeswaran.ecommerce.saga.infrastructure.SagaRepository;
import com.rajeswaran.ecommerce.saga.model.PlaceOrderSaga;
import com.rajeswaran.ecommerce.saga.model.SagaState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;

/**
 * Deadline for each step. A saga stuck waiting (e.g. a participant is down) is flagged to abort, so when the reply
 * finally arrives the saga compensates instead of carrying on with a stale order.
 */
@Component
class SagaTimeoutWatcher {

    private static final Logger log = LoggerFactory.getLogger(SagaTimeoutWatcher.class);

    private final SagaRepository sagas;
    private final Duration timeout;

    SagaTimeoutWatcher(SagaRepository sagas, @Value("${saga.step-timeout:60s}") Duration timeout) {
        this.sagas = sagas;
        this.timeout = timeout;
    }

    @Scheduled(fixedDelay = 10_000)
    @Transactional
    public void flagTimedOutSagas() {
        var waiting = EnumSet.of(SagaState.AWAITING_RESERVATION, SagaState.AWAITING_PAYMENT);
        for (var saga : sagas.findByStateInAndAbortRequestedFalseAndUpdatedAtBefore(waiting, Instant.now().minus(timeout))) {
            log.warn("Saga {} for order {} timed out in {}", saga.id(), saga.orderId(), saga.state());
            PlaceOrderSaga.abort(saga, "Timed out after " + timeout.toSeconds() + "s in " + saga.state());
        }
    }
}
