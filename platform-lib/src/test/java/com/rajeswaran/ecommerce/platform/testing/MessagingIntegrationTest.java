package com.rajeswaran.ecommerce.platform.testing;

import com.rajeswaran.ecommerce.contracts.Message;
import com.rajeswaran.ecommerce.contracts.MessageTypes;
import com.rajeswaran.ecommerce.platform.messaging.IncomingMessage;
import com.rajeswaran.ecommerce.platform.messaging.MessageCodec;
import com.rajeswaran.ecommerce.platform.messaging.MessageHeaders;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for service integration tests: real Postgres and Kafka (Testcontainers, started once per JVM),
 * plus helpers to send a contract message the way the outbox relay does and to read what a service published.
 * Subclasses add {@code @SpringBootTest(properties = "eureka.client.enabled=false")}.
 */
public abstract class MessagingIntegrationTest {

    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6");

    @ServiceConnection
    protected static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.3.1");

    static {
        POSTGRES.start();
        KAFKA.start();
    }

    @Autowired
    protected MessageCodec codec;

    /** Publishes a message with an explicit id, so tests can simulate Kafka redelivering the same message. */
    protected void send(String topic, Message message, UUID messageId) {
        try (var producer = new KafkaProducer<String, String>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class))) {
            var record = new ProducerRecord<>(topic, message.orderId().toString(), codec.encode(message));
            record.headers().add(MessageHeaders.MESSAGE_ID, messageId.toString().getBytes(StandardCharsets.UTF_8));
            record.headers().add(MessageHeaders.MESSAGE_TYPE, MessageTypes.nameOf(message).getBytes(StandardCharsets.UTF_8));
            producer.send(record).get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Reads everything published on {@code topic} for {@code orderId}. Returns once at least {@code expected}
     * messages were seen and nothing new arrived for {@code settle} (so duplicates would be caught), or at timeout.
     */
    protected List<Message> published(String topic, UUID orderId, int expected, Duration settle) {
        var found = new ArrayList<Message>();
        try (var consumer = new KafkaConsumer<String, String>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "it-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))) {
            consumer.subscribe(List.of(topic));
            Instant deadline = Instant.now().plusSeconds(30);
            Instant quietUntil = null;
            while (Instant.now().isBefore(deadline)) {
                for (var record : consumer.poll(Duration.ofMillis(250))) {
                    if (orderId.toString().equals(record.key())) {
                        codec.decode(record).map(IncomingMessage::payload).ifPresent(found::add);
                        quietUntil = null;
                    }
                }
                if (found.size() >= expected) {
                    if (quietUntil == null) {
                        quietUntil = Instant.now().plus(settle);
                    } else if (Instant.now().isAfter(quietUntil)) {
                        break;
                    }
                }
            }
        }
        return found;
    }
}
