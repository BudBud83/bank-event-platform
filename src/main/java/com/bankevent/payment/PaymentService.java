package com.bankevent.payment;

import com.bankevent.account.Account;
import com.bankevent.account.AccountRepository;
import com.bankevent.api.dto.InitiatePaymentRequest;
import com.bankevent.api.dto.PaymentResponse;
import com.bankevent.events.PaymentInitiatedEvent;
import com.bankevent.kafka.KafkaEventPublisher;
import com.bankevent.kafka.Topics;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Accepts a payment request from the API, stores it as INITIATED, and
 * publishes {@link PaymentInitiatedEvent}. That is the only Kafka write
 * this class makes — fraud and ledger continue the story asynchronously.
 *
 * <p>This split is the heart of event-driven banking: the HTTP thread does
 * not wait for fraud screening or booking. The client gets a payment id
 * immediately and later polls GET /payments/{id} (or listens to webhooks).
 */
@Service
public class PaymentService {

    private final PaymentRepository payments;
    private final AccountRepository accounts;
    private final KafkaEventPublisher publisher;

    public PaymentService(
            PaymentRepository payments,
            AccountRepository accounts,
            KafkaEventPublisher publisher
    ) {
        this.payments = payments;
        this.accounts = accounts;
        this.publisher = publisher;
    }

    @Transactional
    public PaymentResponse initiate(InitiatePaymentRequest request) {
        // Idempotency: same key => same payment. Never debit twice for a retry.
        return payments.findByIdempotencyKey(request.idempotencyKey())
                .map(PaymentResponse::from)
                .orElseGet(() -> createNew(request));
    }

    private PaymentResponse createNew(InitiatePaymentRequest request) {
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and destination must differ");
        }

        Account source = accounts.findById(request.sourceAccountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Source account not found"));
        Account destination = accounts.findById(request.destinationAccountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination account not found"));

        if (!source.getCurrency().equals(destination.getCurrency())
                || !source.getCurrency().equals(request.currency().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency mismatch");
        }

        String paymentId = UUID.randomUUID().toString();
        String correlationId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        Payment payment = new Payment(
                paymentId,
                request.idempotencyKey(),
                source.getId(),
                destination.getId(),
                request.amount(),
                request.currency().toUpperCase(),
                request.reference(),
                correlationId,
                now
        );
        payments.save(payment);

        PaymentInitiatedEvent event = new PaymentInitiatedEvent(
                UUID.randomUUID().toString(),
                now,
                correlationId,
                paymentId,
                source.getId(),
                destination.getId(),
                request.amount(),
                payment.getCurrency(),
                request.reference()
        );

        // Keyed by source account: transfers from the same account stay ordered.
        publisher.publish(Topics.PAYMENTS_INITIATED, source.getId(), event);

        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(String paymentId) {
        return PaymentResponse.from(payments.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment not found: " + paymentId)));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> list() {
        return payments.findAll().stream().map(PaymentResponse::from).toList();
    }
}
