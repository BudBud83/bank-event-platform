package com.bankevent.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Core banking account — the source of truth for balances in this demo.
 *
 * <p>{@code @Entity} tells Hibernate: map this class to a database table.
 * Kafka events are the *broadcast* of what happened; this table is the
 * *book* we would show a regulator.
 *
 * <p>{@code @Version} enables optimistic locking. If two ledger workers try
 * to update the same account at once, one succeeds and the other gets
 * {@code OptimisticLockException} and can retry. That is how we avoid
 * lost updates without locking the row for a long time.
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private String id;

    @Column(nullable = false)
    private String ownerName;

    /** ISO-4217 code. We never mix currencies in one transfer in this demo. */
    @Column(nullable = false, length = 3)
    private String currency;

    /**
     * ALWAYS {@link BigDecimal} for money. {@code double} cannot represent
     * 0.10 exactly and would slowly corrupt a ledger.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false)
    private Instant openedAt;

    @Version
    private Long version;

    /** JPA requires a no-arg constructor. */
    protected Account() {
    }

    public Account(String id, String ownerName, String currency, BigDecimal balance, Instant openedAt) {
        this.id = id;
        this.ownerName = ownerName;
        this.currency = currency;
        this.balance = balance;
        this.openedAt = openedAt;
    }

    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public boolean hasSufficientFunds(BigDecimal amount) {
        return balance.compareTo(amount) >= 0;
    }

    public String getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Long getVersion() {
        return version;
    }
}
