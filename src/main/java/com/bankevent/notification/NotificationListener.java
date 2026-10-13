package com.bankevent.notification;

import com.bankevent.events.AccountOpenedEvent;
import com.bankevent.events.FraudAlertEvent;
import com.bankevent.events.NotificationEvent;
import com.bankevent.events.PaymentFailedEvent;
import com.bankevent.events.PaymentSettledEvent;
import com.bankevent.kafka.KafkaEventPublisher;
import com.bankevent.kafka.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Turns domain facts into customer-facing messages.
 *
 * <p>One class, several {@code @KafkaListener} methods, same group id
 * {@code notification-service}. Spring registers each method as a listener
 * on its topic. In a real bank this would call an email/SMS gateway;
 * we publish a {@link NotificationEvent} and log it so you can watch the chain.
 */
@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final KafkaEventPublisher publisher;

    public NotificationListener(KafkaEventPublisher publisher) {
        this.publisher = publisher;
    }

    @KafkaListener(topics = Topics.ACCOUNT_EVENTS, groupId = "notification-service")
    public void onAccountOpened(AccountOpenedEvent event) {
        notify(
                event.correlationId(),
                event.accountId(),
                "Welcome",
                "Account " + event.accountId() + " opened for " + event.ownerName()
                        + " with balance " + event.openingBalance() + " " + event.currency()
        );
    }

    @KafkaListener(topics = Topics.PAYMENTS_SETTLED, groupId = "notification-service")
    public void onSettled(PaymentSettledEvent event) {
        notify(
                event.correlationId(),
                event.sourceAccountId(),
                "Payment sent",
                "Payment " + event.paymentId() + " of " + event.amount() + " " + event.currency()
                        + " settled. New balance: " + event.sourceBalanceAfter()
        );
        notify(
                event.correlationId(),
                event.destinationAccountId(),
                "Payment received",
                "You received " + event.amount() + " " + event.currency()
                        + ". New balance: " + event.destinationBalanceAfter()
        );
    }

    @KafkaListener(topics = Topics.PAYMENTS_FAILED, groupId = "notification-service")
    public void onFailed(PaymentFailedEvent event) {
        notify(
                event.correlationId(),
                event.paymentId(),
                "Payment failed",
                "Payment " + event.paymentId() + " failed: " + event.reason() + " — " + event.detail()
        );
    }

    @KafkaListener(topics = Topics.FRAUD_ALERTS, groupId = "notification-service")
    public void onFraud(FraudAlertEvent event) {
        notify(
                event.correlationId(),
                event.sourceAccountId(),
                "Payment held",
                "Payment " + event.paymentId() + " was flagged (" + event.rule() + "): "
                        + event.explanation()
        );
    }

    private void notify(String correlationId, String accountId, String subject, String body) {
        NotificationEvent message = new NotificationEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                correlationId,
                accountId,
                "LOG",
                subject,
                body
        );
        publisher.publish(Topics.NOTIFICATIONS, accountId, message);
        log.info("NOTIFY [{}] {}: {}", accountId, subject, body);
    }
}
