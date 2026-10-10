package com.rajeswaran.ecommerce.payment.interfaces.messaging;

import com.rajeswaran.ecommerce.contracts.Topics;
import com.rajeswaran.ecommerce.contracts.payment.PaymentCommand;
import com.rajeswaran.ecommerce.payment.application.PaymentCommandHandler;
import com.rajeswaran.ecommerce.platform.messaging.IdempotentMessageHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class PaymentCommandListener {

    private final IdempotentMessageHandler idempotent;
    private final PaymentCommandHandler handler;

    PaymentCommandListener(IdempotentMessageHandler idempotent, PaymentCommandHandler handler) {
        this.idempotent = idempotent;
        this.handler = handler;
    }

    @KafkaListener(topics = Topics.PAYMENT_COMMANDS)
    void on(ConsumerRecord<String, String> record) {
        idempotent.handle(record, message -> {
            if (message instanceof PaymentCommand command) {
                handler.handle(command);
            }
        });
    }
}
