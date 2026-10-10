package com.rajeswaran.ecommerce.order;

import com.rajeswaran.ecommerce.contracts.Topics;
import com.rajeswaran.ecommerce.contracts.order.ConfirmOrder;
import com.rajeswaran.ecommerce.contracts.order.OrderConfirmed;
import com.rajeswaran.ecommerce.contracts.order.OrderPlaced;
import com.rajeswaran.ecommerce.order.application.OrderQueries;
import com.rajeswaran.ecommerce.order.application.OrderingService;
import com.rajeswaran.ecommerce.order.domain.CustomerId;
import com.rajeswaran.ecommerce.order.domain.Money;
import com.rajeswaran.ecommerce.order.domain.PriceCatalog;
import com.rajeswaran.ecommerce.platform.testing.MessagingIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;

@SpringBootTest(properties = "eureka.client.enabled=false")
class OrderMessagingIntegrationTest extends MessagingIntegrationTest {

    @MockitoBean // replaces the synchronous call to Inventory
    PriceCatalog priceCatalog;

    @Autowired
    OrderingService ordering;

    @Autowired
    OrderQueries queries;

    @Test
    void placingAnOrderPublishesOrderPlacedAndSagaCommandsAreIdempotent() {
        given(priceCatalog.priceOf(anyCollection())).willReturn(Map.of("KB-001",
                new PriceCatalog.PricedItem("KB-001", "Keyboard", new Money(new BigDecimal("89.00"), "USD"))));

        UUID orderId = ordering.placeOrder(new CustomerId("alice"),
                List.of(new OrderingService.RequestedLine("KB-001", 2))).id().value();

        assertThat(published(Topics.ORDER_EVENTS, orderId, 1, Duration.ofMillis(500)))
                .singleElement().isInstanceOfSatisfying(OrderPlaced.class,
                        e -> assertThat(e.total()).isEqualByComparingTo("178.00"));

        // Two distinct ConfirmOrder messages (e.g. orchestrator retried): business-level idempotency
        send(Topics.ORDER_COMMANDS, new ConfirmOrder(orderId), UUID.randomUUID());
        send(Topics.ORDER_COMMANDS, new ConfirmOrder(orderId), UUID.randomUUID());

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(queries.byId(orderId).status()).isEqualTo("CONFIRMED"));
        assertThat(published(Topics.ORDER_EVENTS, orderId, 2, Duration.ofSeconds(2)))
                .filteredOn(OrderConfirmed.class::isInstance).hasSize(1);
    }
}
