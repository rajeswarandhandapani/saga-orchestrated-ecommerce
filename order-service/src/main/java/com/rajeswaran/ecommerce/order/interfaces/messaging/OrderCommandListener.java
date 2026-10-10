package com.rajeswaran.ecommerce.order.interfaces.messaging;

import com.rajeswaran.ecommerce.contracts.Topics;
import com.rajeswaran.ecommerce.contracts.order.OrderCommand;
import com.rajeswaran.ecommerce.order.application.OrderingService;
import com.rajeswaran.ecommerce.platform.messaging.IdempotentMessageHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class OrderCommandListener {

    private final IdempotentMessageHandler idempotent;
    private final OrderingService ordering;

    OrderCommandListener(IdempotentMessageHandler idempotent, OrderingService ordering) {
        this.idempotent = idempotent;
        this.ordering = ordering;
    }

    @KafkaListener(topics = Topics.ORDER_COMMANDS)
    void on(ConsumerRecord<String, String> record) {
        idempotent.handle(record, message -> {
            if (message instanceof OrderCommand command) {
                ordering.handle(command);
            }
        });
    }
}
