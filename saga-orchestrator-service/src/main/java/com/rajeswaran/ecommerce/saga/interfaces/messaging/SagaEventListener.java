package com.rajeswaran.ecommerce.saga.interfaces.messaging;

import com.rajeswaran.ecommerce.contracts.Topics;
import com.rajeswaran.ecommerce.platform.messaging.IdempotentMessageHandler;
import com.rajeswaran.ecommerce.saga.application.SagaCoordinator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** The orchestrator listens to the event topics of every participant and to nothing else. */
@Component
class SagaEventListener {

    private final IdempotentMessageHandler idempotent;
    private final SagaCoordinator coordinator;

    SagaEventListener(IdempotentMessageHandler idempotent, SagaCoordinator coordinator) {
        this.idempotent = idempotent;
        this.coordinator = coordinator;
    }

    @KafkaListener(topics = {Topics.ORDER_EVENTS, Topics.INVENTORY_EVENTS, Topics.PAYMENT_EVENTS})
    void on(ConsumerRecord<String, String> record) {
        idempotent.handle(record, coordinator::on);
    }
}
