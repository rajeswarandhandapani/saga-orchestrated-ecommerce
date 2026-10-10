package com.rajeswaran.ecommerce.platform.messaging;

import com.rajeswaran.ecommerce.contracts.Message;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Consumer;

/**
 * The one way services consume Kafka records:
 * decode → open a transaction → skip if already processed (inbox) → run the handler → commit.
 * Any outbox messages the handler appends are committed atomically with the inbox row and the state change.
 * If the handler throws, everything rolls back and the Kafka error handler retries the record.
 */
public class IdempotentMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(IdempotentMessageHandler.class);

    private final MessageCodec codec;
    private final InboxGuard inbox;
    private final TransactionTemplate tx;

    public IdempotentMessageHandler(MessageCodec codec, InboxGuard inbox, TransactionTemplate tx) {
        this.codec = codec;
        this.inbox = inbox;
        this.tx = tx;
    }

    public void handle(ConsumerRecord<String, String> record, Consumer<Message> handler) {
        var decoded = codec.decode(record);
        if (decoded.isEmpty()) {
            log.warn("Skipping record with unknown/missing type on {} key={}", record.topic(), record.key());
            return;
        }
        IncomingMessage message = decoded.get();
        if (message.correlationId() != null) {
            MDC.put(MessageHeaders.CORRELATION_MDC_KEY, message.correlationId());
        }
        try {
            tx.executeWithoutResult(status -> {
                if (!inbox.firstTime(message.messageId(), message.type())) {
                    log.info("Duplicate {} {} ignored", message.type(), message.messageId());
                    return;
                }
                log.info("Handling {} for order {}", message.type(), message.payload().orderId());
                handler.accept(message.payload());
            });
        } finally {
            MDC.remove(MessageHeaders.CORRELATION_MDC_KEY);
        }
    }
}
