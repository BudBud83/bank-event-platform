package com.bankevent.api.dto;

import com.bankevent.account.Account;

import java.math.BigDecimal;
import java.time.Instant;

/** JSON we return to API clients. Keep entities internal; expose a stable DTO. */
public record AccountResponse(
        String accountId,
        String ownerName,
        String currency,
        BigDecimal balance,
        Instant openedAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getOwnerName(),
                account.getCurrency(),
                account.getBalance(),
                account.getOpenedAt()
        );
    }
}
