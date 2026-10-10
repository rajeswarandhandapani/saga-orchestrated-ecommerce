package com.rajeswaran.ecommerce.inventory.interfaces.messaging;

import com.rajeswaran.ecommerce.contracts.Topics;
import com.rajeswaran.ecommerce.contracts.inventory.InventoryCommand;
import com.rajeswaran.ecommerce.inventory.application.InventoryCommandHandler;
import com.rajeswaran.ecommerce.platform.messaging.IdempotentMessageHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Inbound adapter: Kafka → application service. Inventory only listens to its own command topic. */
@Component
class InventoryCommandListener {

    private final IdempotentMessageHandler idempotent;
    private final InventoryCommandHandler handler;

    InventoryCommandListener(IdempotentMessageHandler idempotent, InventoryCommandHandler handler) {
        this.idempotent = idempotent;
        this.handler = handler;
    }

    @KafkaListener(topics = Topics.INVENTORY_COMMANDS)
    void on(ConsumerRecord<String, String> record) {
        idempotent.handle(record, message -> {
            if (message instanceof InventoryCommand command) {
                handler.handle(command);
            }
        });
    }
}
