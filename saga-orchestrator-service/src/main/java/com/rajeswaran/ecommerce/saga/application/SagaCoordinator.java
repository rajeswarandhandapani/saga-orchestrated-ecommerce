package com.rajeswaran.ecommerce.saga.application;

import com.rajeswaran.ecommerce.contracts.Command;
import com.rajeswaran.ecommerce.contracts.Event;
import com.rajeswaran.ecommerce.contracts.Message;
import com.rajeswaran.ecommerce.contracts.Topics;
import com.rajeswaran.ecommerce.contracts.inventory.InventoryCommand;
import com.rajeswaran.ecommerce.contracts.order.OrderCommand;
import com.rajeswaran.ecommerce.contracts.order.OrderPlaced;
import com.rajeswaran.ecommerce.contracts.payment.PaymentCommand;
import com.rajeswaran.ecommerce.platform.messaging.OutboxPublisher;
import com.rajeswaran.ecommerce.saga.infrastructure.SagaRepository;
import com.rajeswaran.ecommerce.saga.model.PlaceOrderSaga;
import com.rajeswaran.ecommerce.saga.model.SagaInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Glue between Kafka and the pure {@link PlaceOrderSaga}: load state → apply event → save state and
 * append the resulting commands to the outbox, all in the listener's transaction.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class SagaCoordinator {

    private static final Logger log = LoggerFactory.getLogger(SagaCoordinator.class);

    private final SagaRepository sagas;
    private final OutboxPublisher outbox;

    public SagaCoordinator(SagaRepository sagas, OutboxPublisher outbox) {
        this.sagas = sagas;
        this.outbox = outbox;
    }

    public void on(Message message) {
        if (!(message instanceof Event event)) {
            return;
        }
        if (event instanceof OrderPlaced placed) {
            if (sagas.findByOrderId(placed.orderId()).isPresent()) {
                log.info("Saga for order {} already started", placed.orderId());
                return;
            }
            PlaceOrderSaga.Started started = PlaceOrderSaga.start(placed);
            sagas.save(started.saga());
            dispatch(started.commands());
            return;
        }
        sagas.findByOrderId(event.orderId()).ifPresentOrElse(saga -> {
            List<Command> commands = PlaceOrderSaga.on(saga, event);
            sagas.save(saga);
            dispatch(commands);
            log.info("Saga {} order {} now {}", saga.id(), saga.orderId(), saga.state());
        }, () -> log.warn("No saga for order {} - ignoring {}", event.orderId(), event.getClass().getSimpleName()));
    }

    private void dispatch(List<Command> commands) {
        for (Command command : commands) {
            outbox.append(topicFor(command), command);
        }
    }

    /** Commands go to the command topic of the context that owns the behaviour. */
    static String topicFor(Command command) {
        return switch (command) {
            case OrderCommand c -> Topics.ORDER_COMMANDS;
            case InventoryCommand c -> Topics.INVENTORY_COMMANDS;
            case PaymentCommand c -> Topics.PAYMENT_COMMANDS;
            default -> throw new IllegalArgumentException("Unroutable command " + command);
        };
    }
}
