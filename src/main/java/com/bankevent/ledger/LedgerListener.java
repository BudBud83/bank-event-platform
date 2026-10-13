package com.bankevent.ledger;

import com.bankevent.account.Account;
import com.bankevent.account.AccountRepository;
import com.bankevent.events.PaymentApprovedEvent;
import com.bankevent.events.PaymentFailedEvent;
import com.bankevent.events.PaymentSettledEvent;
import com.bankevent.kafka.KafkaEventPublisher;
import com.bankevent.kafka.Topics;
import com.bankevent.payment.Payment;
import com.bankevent.payment.PaymentRepository;
import com.bankevent.payment.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * The "core ledger" department. It is the only component allowed to change
 * balances after a payment is approved.
 *
 * <p>Double-entry bookkeeping in one transaction:
 * debit source, credit destination. If anything fails, the whole DB
 * transaction rolls back — you never want a debit without the matching credit.
 *
 * <p>Kafka delivery is <b>at-least-once</b>: a crash after booking but before
 * offset commit would redeliver the same {@code PaymentApprovedEvent}.
 * We therefore treat SETTLED/FAILED payments as already done (idempotent consumer).
 */
@Component
public class LedgerListener {

    private static final Logger log = LoggerFactory.getLogger(LedgerListener.class);

    private final AccountRepository accounts;
    private final PaymentRepository payments;
    private final KafkaEventPublisher publisher;

    public LedgerListener(
            AccountRepository accounts,
            PaymentRepository payments,
            KafkaEventPublisher publisher
    ) {
        this.accounts = accounts;
        this.payments = payments;
        this.publisher = publisher;
    }

    @Transactional
    @KafkaListener(topics = Topics.PAYMENTS_APPROVED, groupId = "ledger-service")
    public void onPaymentApproved(PaymentApprovedEvent event) {
        log.info("Ledger booking payment {}", event.paymentId());

        Payment payment = payments.findById(event.paymentId()).orElse(null);
        if (payment == null) {
            log.warn("Unknown payment {}, skipping", event.paymentId());
            return;
        }

        // Idempotency: do not book twice if Kafka redelivers.
        if (payment.getStatus() == PaymentStatus.SETTLED
                || payment.getStatus() == PaymentStatus.FAILED) {
            log.info("Payment {} already {}, ignoring redelivery", payment.getId(), payment.getStatus());
            return;
        }

        Account source = accounts.findById(event.sourceAccountId()).orElse(null);
        Account destination = accounts.findById(event.destinationAccountId()).orElse(null);

        if (source == null || destination == null) {
            fail(payment, event, "ACCOUNT_NOT_FOUND", "Source or destination missing");
            return;
        }

        if (!source.hasSufficientFunds(event.amount())) {
            fail(payment, event, "INSUFFICIENT_FUNDS",
                    "Source balance " + source.getBalance() + " < " + event.amount());
            return;
        }

        source.debit(event.amount());
        destination.credit(event.amount());
        payment.markSettled();

        publisher.publish(
                Topics.PAYMENTS_SETTLED,
                source.getId(),
                new PaymentSettledEvent(
                        UUID.randomUUID().toString(),
                        Instant.now(),
                        event.correlationId(),
                        event.paymentId(),
                        source.getId(),
                        destination.getId(),
                        event.amount(),
                        event.currency(),
                        source.getBalance(),
                        destination.getBalance()
                )
        );
    }

    private void fail(Payment payment, PaymentApprovedEvent event, String reason, String detail) {
        payment.markFailed(reason);
        publisher.publish(
                Topics.PAYMENTS_FAILED,
                event.sourceAccountId(),
                new PaymentFailedEvent(
                        UUID.randomUUID().toString(),
                        Instant.now(),
                        event.correlationId(),
                        event.paymentId(),
                        reason,
                        detail
                )
        );
    }
}
