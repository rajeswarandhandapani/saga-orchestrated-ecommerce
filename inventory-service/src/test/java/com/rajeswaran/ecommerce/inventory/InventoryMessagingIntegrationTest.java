package com.rajeswaran.ecommerce.inventory;

import com.rajeswaran.ecommerce.contracts.LineItem;
import com.rajeswaran.ecommerce.contracts.Topics;
import com.rajeswaran.ecommerce.contracts.inventory.InventoryReleased;
import com.rajeswaran.ecommerce.contracts.inventory.InventoryReserved;
import com.rajeswaran.ecommerce.contracts.inventory.ReleaseInventory;
import com.rajeswaran.ecommerce.contracts.inventory.ReserveInventory;
import com.rajeswaran.ecommerce.inventory.application.CatalogQueries;
import com.rajeswaran.ecommerce.platform.testing.MessagingIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Kafka in → inbox → domain → outbox → Kafka out, against real Postgres and Kafka. */
@SpringBootTest(properties = "eureka.client.enabled=false")
class InventoryMessagingIntegrationTest extends MessagingIntegrationTest {

    @Autowired
    CatalogQueries queries;

    @Test
    void duplicateDeliveryReservesOnceAndReleaseRestoresStock() {
        UUID orderId = UUID.randomUUID();
        int before = queries.product("MS-001").available();
        var reserve = new ReserveInventory(orderId, List.of(new LineItem("MS-001", 2, null)));
        UUID messageId = UUID.randomUUID();

        send(Topics.INVENTORY_COMMANDS, reserve, messageId);
        send(Topics.INVENTORY_COMMANDS, reserve, messageId); // Kafka redelivery: same message id

        assertThat(published(Topics.INVENTORY_EVENTS, orderId, 1, Duration.ofSeconds(2)))
                .containsExactly(new InventoryReserved(orderId));
        assertThat(queries.product("MS-001").available()).isEqualTo(before - 2);

        send(Topics.INVENTORY_COMMANDS, new ReleaseInventory(orderId, "test"), UUID.randomUUID());

        assertThat(published(Topics.INVENTORY_EVENTS, orderId, 2, Duration.ofSeconds(1)))
                .containsExactly(new InventoryReserved(orderId), new InventoryReleased(orderId));
        assertThat(queries.product("MS-001").available()).isEqualTo(before);
    }
}
