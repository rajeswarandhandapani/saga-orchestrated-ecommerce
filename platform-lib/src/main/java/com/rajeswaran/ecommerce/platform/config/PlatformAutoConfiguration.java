package com.rajeswaran.ecommerce.platform.config;

import com.rajeswaran.ecommerce.contracts.Topics;
import com.rajeswaran.ecommerce.platform.messaging.*;
import com.rajeswaran.ecommerce.platform.web.ApiExceptionHandler;
import com.rajeswaran.ecommerce.platform.web.CorrelationIdFilter;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.databind.json.JsonMapper;

/** Wires the outbox/inbox messaging infrastructure into every service that depends on platform-lib. */
@AutoConfiguration
@EnableScheduling
public class PlatformAutoConfiguration {

    @Bean
    MessageCodec messageCodec(JsonMapper jsonMapper) {
        return new MessageCodec(jsonMapper);
    }

    @Bean
    OutboxPublisher outboxPublisher(JdbcTemplate jdbc, MessageCodec codec) {
        return new OutboxPublisher(jdbc, codec);
    }

    @Bean
    OutboxRelay outboxRelay(JdbcTemplate jdbc, KafkaTemplate<String, String> kafka, PlatformTransactionManager txm) {
        return new OutboxRelay(jdbc, kafka, new TransactionTemplate(txm));
    }

    @Bean
    InboxGuard inboxGuard(JdbcTemplate jdbc) {
        return new InboxGuard(jdbc);
    }

    @Bean
    IdempotentMessageHandler idempotentMessageHandler(MessageCodec codec, InboxGuard inbox, PlatformTransactionManager txm) {
        return new IdempotentMessageHandler(codec, inbox, new TransactionTemplate(txm));
    }

    /** Retry a failing record 3 times, then park it on {@code <topic>-dlt} instead of blocking the partition. */
    @Bean
    CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafka) {
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(kafka), new FixedBackOff(1000L, 3));
    }

    @Bean
    KafkaAdmin.NewTopics ecommerceTopics() {
        return new KafkaAdmin.NewTopics(Topics.ALL.stream()
                .map(name -> TopicBuilder.name(name).partitions(3).replicas(1).build())
                .toArray(NewTopic[]::new));
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    static class ServletWebConfiguration {

        @Bean
        CorrelationIdFilter correlationIdFilter() {
            return new CorrelationIdFilter();
        }

        @Bean
        ApiExceptionHandler apiExceptionHandler() {
            return new ApiExceptionHandler();
        }
    }
}
