package com.rajeswaran.ecommerce.payment;

import com.rajeswaran.ecommerce.contracts.Topics;
import com.rajeswaran.ecommerce.contracts.payment.AuthorizePayment;
import com.rajeswaran.ecommerce.contracts.payment.PaymentAuthorized;
import com.rajeswaran.ecommerce.contracts.payment.PaymentDeclined;
import com.rajeswaran.ecommerce.platform.testing.MessagingIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "eureka.client.enabled=false")
class PaymentMessagingIntegrationTest extends MessagingIntegrationTest {

    @Test
    void authorizesSmallAmountsOnceEvenWhenRedelivered() {
        UUID orderId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        var command = new AuthorizePayment(orderId, "alice", new BigDecimal("50.00"), "USD");

        send(Topics.PAYMENT_COMMANDS, command, messageId);
        send(Topics.PAYMENT_COMMANDS, command, messageId);

        assertThat(published(Topics.PAYMENT_EVENTS, orderId, 1, Duration.ofSeconds(2)))
                .singleElement().isInstanceOf(PaymentAuthorized.class);
    }

    @Test
    void declinesAmountsAboveTheGatewayLimit() {
        UUID orderId = UUID.randomUUID();
        send(Topics.PAYMENT_COMMANDS, new AuthorizePayment(orderId, "alice", new BigDecimal("1299.00"), "USD"), UUID.randomUUID());

        assertThat(published(Topics.PAYMENT_EVENTS, orderId, 1, Duration.ofSeconds(1)))
                .singleElement().isInstanceOf(PaymentDeclined.class);
    }
}
