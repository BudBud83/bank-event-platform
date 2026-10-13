package com.bankevent.account;

import com.bankevent.api.dto.AccountResponse;
import com.bankevent.api.dto.CreateAccountRequest;
import com.bankevent.events.AccountOpenedEvent;
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
 * Application service for accounts.
 *
 * <p>{@code @Service} is a stereotype for Spring: it becomes a singleton bean
 * that controllers can inject.
 *
 * <p>{@code @Transactional} wraps the method in a database transaction.
 * If publishing to Kafka threw after {@code save}, the DB write would roll back
 * (we publish after save so the common case is: DB commit then event).
 * A production bank uses an outbox table so DB and Kafka cannot diverge.
 * We keep it simple here and call that out in the README.
 */
@Service
public class AccountService {

    private final AccountRepository accounts;
    private final KafkaEventPublisher publisher;

    public AccountService(AccountRepository accounts, KafkaEventPublisher publisher) {
        this.accounts = accounts;
        this.publisher = publisher;
    }

    @Transactional
    public AccountResponse open(CreateAccountRequest request) {
        String accountId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        Account account = new Account(
                accountId,
                request.ownerName().trim(),
                request.currency().toUpperCase(),
                request.openingBalance(),
                now
        );
        accounts.save(account);

        // correlationId = eventId for the first hop of a new account
        String eventId = UUID.randomUUID().toString();
        AccountOpenedEvent event = new AccountOpenedEvent(
                eventId,
                now,
                eventId,
                accountId,
                account.getOwnerName(),
                account.getCurrency(),
                account.getBalance()
        );

        // Key = accountId so all future events for this account share a partition.
        publisher.publish(Topics.ACCOUNT_EVENTS, accountId, event);

        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse get(String accountId) {
        return AccountResponse.from(require(accountId));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> list() {
        return accounts.findAll().stream().map(AccountResponse::from).toList();
    }

    public Account require(String accountId) {
        return accounts.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found: " + accountId));
    }
}
