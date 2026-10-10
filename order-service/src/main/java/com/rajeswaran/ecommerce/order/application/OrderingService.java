package com.rajeswaran.ecommerce.order.application;

import com.rajeswaran.ecommerce.contracts.LineItem;
import com.rajeswaran.ecommerce.contracts.Topics;
import com.rajeswaran.ecommerce.contracts.order.*;
import com.rajeswaran.ecommerce.order.domain.*;
import com.rajeswaran.ecommerce.order.domain.Order;
import com.rajeswaran.ecommerce.order.domain.PriceCatalog.PricedItem;
import com.rajeswaran.ecommerce.platform.domain.DomainEvent;
import com.rajeswaran.ecommerce.platform.domain.DomainException;
import com.rajeswaran.ecommerce.platform.domain.NotFoundException;
import com.rajeswaran.ecommerce.platform.messaging.OutboxPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Use cases of the Ordering context.
 * <p>
 * Placing an order mixes both styles of integration:
 * <ol>
 *   <li><b>sync</b> - ask Inventory for current prices (we need them <i>now</i> to build a valid Order);</li>
 *   <li><b>async</b> - save the PENDING order and an {@code OrderPlaced} event in one transaction; the saga
 *       orchestrator picks it up from Kafka and drives reservation and payment.</li>
 * </ol>
 * The remote call happens before the transaction is opened so no DB connection is held while waiting on HTTP.
 */
@Service
public class OrderingService {

    private static final Logger log = LoggerFactory.getLogger(OrderingService.class);

    public record RequestedLine(String sku, int quantity) {
    }

    private final Orders orders;
    private final PriceCatalog priceCatalog;
    private final OutboxPublisher outbox;
    private final TransactionTemplate tx;

    public OrderingService(Orders orders, PriceCatalog priceCatalog, OutboxPublisher outbox,
                           PlatformTransactionManager txManager) {
        this.orders = orders;
        this.priceCatalog = priceCatalog;
        this.outbox = outbox;
        this.tx = new TransactionTemplate(txManager);
    }

    public Order placeOrder(CustomerId customer, List<RequestedLine> requested) {
        Map<String, PricedItem> prices = priceCatalog.priceOf(requested.stream().map(l -> l.sku().toUpperCase()).toList());
        List<OrderLine> lines = requested.stream().map(r -> {
            PricedItem item = prices.get(r.sku().toUpperCase());
            if (item == null) {
                throw new DomainException("Unknown product " + r.sku());
            }
            return new OrderLine(item.sku(), item.name(), r.quantity(), item.unitPrice());
        }).toList();

        Order order = Order.place(customer, lines);
        tx.executeWithoutResult(s -> {
            orders.save(order);
            publish(order.pullDomainEvents());
        });
        log.info("Order {} placed by {} total {}", order.id(), customer.value(), order.total());
        return order;
    }

    public void requestCancellation(UUID orderId, CustomerId requester) {
        tx.executeWithoutResult(s -> {
            Order order = load(orderId);
            order.requestCancellation(requester);
            orders.save(order);
            publish(order.pullDomainEvents());
        });
    }

    /** Commands from the saga orchestrator. Runs inside the inbox transaction opened by the listener. */
    public void handle(OrderCommand command) {
        Order order = load(command.orderId());
        switch (command) {
            case ConfirmOrder c -> order.confirm();
            case RejectOrder c -> order.reject(c.reason());
            case CancelOrder c -> order.cancel(c.reason());
        }
        orders.save(order);
        publish(order.pullDomainEvents());
    }

    private Order load(UUID orderId) {
        return orders.findById(new OrderId(orderId)).orElseThrow(() -> new NotFoundException("No order " + orderId));
    }

    /** Domain events → integration events (Published Language). */
    private void publish(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            Object message = switch (event) {
                case OrderEvents.OrderPlaced e -> toContract(e.order());
                case OrderEvents.CancellationRequested e -> new OrderCancellationRequested(e.orderId().value());
                case OrderEvents.OrderConfirmed e -> new OrderConfirmed(e.orderId().value());
                case OrderEvents.OrderRejected e -> new OrderRejected(e.orderId().value(), e.reason());
                case OrderEvents.OrderCancelled e -> new OrderCancelled(e.orderId().value(), e.reason());
                default -> null;
            };
            if (message instanceof OrderEvent orderEvent) {
                outbox.append(Topics.ORDER_EVENTS, orderEvent);
            }
        }
    }

    private static com.rajeswaran.ecommerce.contracts.order.OrderPlaced toContract(Order order) {
        Money total = order.total();
        return new com.rajeswaran.ecommerce.contracts.order.OrderPlaced(order.id().value(), order.customerId().value(),
                order.lines().stream().map(l -> new LineItem(l.sku(), l.quantity(), l.unitPrice().amount())).toList(),
                total.amount(), total.currency());
    }
}
