# Bank Event Platform

A teaching project: a **core-banking payment pipeline** in **Java 17**, **Spring Boot 3**, and **Apache Kafka**.

You are looking at a simplified version of how a bank moves money without one giant synchronous method. The HTTP API accepts work, Kafka carries facts between departments, and each department is a consumer group that can fail, scale, or restart on its own.

## What you will learn

| Piece | Role in a bank | Role in this repo |
| --- | --- | --- |
| REST (Spring MVC) | Internet / mobile / branch channel | `AccountController`, `PaymentController` |
| JPA + H2 | Core books (balances) | `Account`, `Payment` entities |
| Kafka producer | Publish a fact: "this happened" | `KafkaEventPublisher` |
| Kafka topic | Durable log of one kind of fact | `Topics` + `KafkaTopicConfig` |
| Consumer group | One department reading that log | `fraud-service`, `ledger-service`, `notification-service`, `audit-service` |
| Event key | Keep one account's events in order | account id on every send |

Every class has comments that explain *why* it exists, not only *what* it does. Start with `BankEventPlatformApplication`, then `PaymentService`, then the listeners.

## How the components work together

```
  Mobile / curl
        |
        v
  Spring MVC  ----save---->  H2 (accounts, payments)
        |
        |  KafkaTemplate  (acks=all, key=accountId)
        v
  bank.payments.initiated
        |
        +---- group fraud-service ----> FraudDetectionListener
        |                                      |
        |                         pass         | fail
        |                          v           v
        |              bank.payments.approved   bank.fraud.alerts
        |                          |                    |
        |                          v                    v
        |                 LedgerListener          NotificationListener
        |                 (debit + credit)              |
        |                    |        |                 |
        |                    v        v                 v
        |         payments.settled  payments.failed   bank.notifications
        |
        +---- group audit-service ----> AuditTrailListener (copy of everything)
        +---- group notification-service ----> emails/SMS (logged only)
```

### Walk through one $50 transfer

1. **POST /api/accounts** twice (Alice, Bob). `AccountService` writes rows and publishes `AccountOpenedEvent` to `bank.account.events`.
2. **POST /api/payments**. `PaymentService` stores status `INITIATED` and publishes `PaymentInitiatedEvent`. The HTTP response is `202 Accepted` — money has **not** moved yet.
3. **Fraud** (`groupId = fraud-service`) reads that event. If the amount is ≤ 10_000 and Alice has not fired more than 3 payments in 60 seconds, it publishes `PaymentApprovedEvent`.
4. **Ledger** (`groupId = ledger-service`) reads the approved event. In one DB transaction it debits Alice, credits Bob, marks the payment `SETTLED`, and publishes `PaymentSettledEvent`. If Alice has too little money it publishes `PaymentFailedEvent` instead.
5. **Notifications** turn those facts into log lines (stand-in for SMS).
6. **Audit** sees every hop because it uses a **different group id**. Kafka delivers a full copy to each group.

Same `correlationId` is copied onto every event so you can grep the logs for one payment end-to-end.

### Why Kafka instead of method calls?

In a monolith you could call `fraud.screen()` then `ledger.book()` in one thread. Banks still use a log because:

- **Fan-out**: fraud, ledger, notifications, and audit all need the same fact. The producer should not know the list.
- **Back-pressure**: if fraud is slow, events wait on the topic. The API stays up.
- **Replay**: a new consumer group can read history (`auto-offset-reset: earliest`).
- **Ordering per account**: the **key** is the account id, so Kafka puts that account's events on one partition. Consumers see them in order.

### Delivery and money safety

Kafka here is **at-least-once**. If the JVM dies after booking but before the offset is committed, the approved event is delivered again. `LedgerListener` therefore **ignores** payments already `SETTLED` or `FAILED`. That pattern is called an **idempotent consumer**. Without it, a retry would debit twice.

`acks: all` on the producer means we wait until the broker has the record before the REST call returns.

A production bank also uses a **transactional outbox** (write the event in the same DB transaction, a relay publishes to Kafka) so a crash cannot commit the payment row without the event. This demo publishes after `save` and documents the gap.

## Prerequisites

- Java 17+
- Maven 3.9+
- Docker Desktop (for Kafka)

This machine may not have Apple Command Line Tools, so `git` / `mvn` wrappers that go through `/usr/bin` can fail until those tools are installed.

## Run

```bash
# 1. Kafka broker on localhost:9092
docker compose up -d

# 2. App (from this directory)
mvn spring-boot:run
```

API: http://localhost:8080  
H2 console: http://localhost:8080/h2-console  
JDBC URL: `jdbc:h2:mem:corebank`  user: `sa`  password: (empty)

## Try the happy path

```bash
# Alice
curl -s -X POST http://localhost:8080/api/accounts \
  -H 'Content-Type: application/json' \
  -d '{"ownerName":"Alice","currency":"USD","openingBalance":1000}'

# Bob (copy Alice's accountId from the previous response)
curl -s -X POST http://localhost:8080/api/accounts \
  -H 'Content-Type: application/json' \
  -d '{"ownerName":"Bob","currency":"USD","openingBalance":50}'

# Transfer 50 — use a unique idempotencyKey per new payment
curl -s -X POST http://localhost:8080/api/payments \
  -H 'Content-Type: application/json' \
  -d '{
    "idempotencyKey":"pay-001",
    "sourceAccountId":"ALICE_ID",
    "destinationAccountId":"BOB_ID",
    "amount":50,
    "currency":"USD",
    "reference":"coffee"
  }'

# Poll until status is SETTLED
curl -s http://localhost:8080/api/payments
curl -s http://localhost:8080/api/accounts
```

Watch the application log. You should see `Published`, `Fraud screening`, `Ledger booking`, `NOTIFY`, and `AUDIT` lines sharing one `correlationId`.

## Try the other paths

**Fraud (high value)** — amount `10000.01` → status `FLAGGED`, topic `bank.fraud.alerts`, balances unchanged.

**Insufficient funds** — Alice opening balance `10`, payment `50` → fraud still approves (amount is small), ledger publishes `INSUFFICIENT_FUNDS`.

**Idempotency** — POST the same `idempotencyKey` twice → one payment row.

**Velocity fraud** — four small payments from Alice within a minute → the fourth is `FLAGGED`.

## Package map (read in this order)

1. `BankEventPlatformApplication` — boots Spring, enables Kafka listeners  
2. `config/KafkaTopicConfig` — creates topics (3 partitions, 1 replica)  
3. `kafka/Topics` + `KafkaEventPublisher` — names and how we send  
4. `events/*` — the facts on the wire (Java records)  
5. `account/*` + `api/AccountController` — open an account  
6. `payment/*` + `api/PaymentController` — start a transfer  
7. `fraud/*` — screen initiated payments  
8. `ledger/LedgerListener` — the only place balances change for payments  
9. `notification/NotificationListener` — customer messages  
10. `audit/AuditTrailListener` — second consumer group, full copy  

## Configuration

`src/main/resources/application.yml`

- `spring.kafka.bootstrap-servers` — where the broker lives  
- producer `acks: all`, JSON serializer, type headers  
- consumer `enable-auto-commit: false` + `ack-mode: record` (commit after the listener returns)  
- `bank.fraud.*` — demo policy numbers  

## Tests

```bash
mvn test
```

`FraudRuleEngineTest` is a pure unit test (no Kafka). Business rules should be tested this way first.

## What we deliberately simplified

- One Spring Boot process instead of four microservices (packages stand in for services)
- H2 instead of PostgreSQL
- One Kafka broker, replication factor 1
- Notifications only log
- No TLS, no SASL, no schema registry (Avro/Protobuf)
- No outbox, no Kafka transactions

Those are the next steps once this pipeline feels obvious.
