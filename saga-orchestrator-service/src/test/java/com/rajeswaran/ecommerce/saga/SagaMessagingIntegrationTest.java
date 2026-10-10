package com.rajeswaran.ecommerce.saga;

import com.rajeswaran.ecommerce.contracts.LineItem;
import com.rajeswaran.ecommerce.contracts.Topics;
import com.rajeswaran.ecommerce.contracts.inventory.InventoryReserved;
import com.rajeswaran.ecommerce.contracts.inventory.ReserveInventory;
import com.rajeswaran.ecommerce.contracts.order.OrderPlaced;
import com.rajeswaran.ecommerce.contracts.payment.AuthorizePayment;
import com.rajeswaran.ecommerce.platform.testing.MessagingIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "eureka.client.enabled=false")
class SagaMessagingIntegrationTest extends MessagingIntegrationTest {

    @Test
    void orchestratorDrivesReservationThenPayment() {
        UUID orderId = UUID.randomUUID();
        send(Topics.ORDER_EVENTS, new OrderPlaced(orderId, "alice",
                List.of(new LineItem("KB-001", 1, new BigDecimal("89.00"))), new BigDecimal("89.00"), "USD"), UUID.randomUUID());

        assertThat(published(Topics.INVENTORY_COMMANDS, orderId, 1, Duration.ofMillis(500)))
                .singleElement().isInstanceOf(ReserveInventory.class);

        send(Topics.INVENTORY_EVENTS, new InventoryReserved(orderId), UUID.randomUUID());

        assertThat(published(Topics.PAYMENT_COMMANDS, orderId, 1, Duration.ofMillis(500)))
                .singleElement().isEqualTo(new AuthorizePayment(orderId, "alice", new BigDecimal("89.00"), "USD"));
    }
}
