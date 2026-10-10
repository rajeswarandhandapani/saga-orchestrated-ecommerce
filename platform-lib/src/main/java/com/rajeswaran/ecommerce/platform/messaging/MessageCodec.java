package com.rajeswaran.ecommerce.platform.messaging;

import com.rajeswaran.ecommerce.contracts.Message;
import com.rajeswaran.ecommerce.contracts.MessageTypes;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * JSON (de)serialisation of contract records. The Kafka value is plain JSON; the type travels in a header,
 * so producers and consumers only share the logical type name, not Java class names.
 */
public class MessageCodec {

    private final JsonMapper jsonMapper;

    public MessageCodec(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public String encode(Message message) {
        return jsonMapper.writeValueAsString(message);
    }

    /** Empty when the record carries a type this service does not know (e.g. a newer message version). */
    public Optional<IncomingMessage> decode(ConsumerRecord<String, String> record) {
        String type = header(record, MessageHeaders.MESSAGE_TYPE);
        String id = header(record, MessageHeaders.MESSAGE_ID);
        if (type == null || id == null) {
            return Optional.empty();
        }
        return MessageTypes.classOf(type).map(clazz -> new IncomingMessage(
                UUID.fromString(id), type, header(record, MessageHeaders.CORRELATION_ID),
                jsonMapper.readValue(record.value(), clazz)));
    }

    private static String header(ConsumerRecord<?, ?> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
