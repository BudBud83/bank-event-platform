package com.bankevent.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * JSON body for POST /api/accounts.
 *
 * <p>{@code @NotBlank} / {@code @DecimalMin} are validated by Spring before
 * the controller method runs. Invalid requests never reach the service.
 */
public record CreateAccountRequest(
        @NotBlank String ownerName,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @DecimalMin(value = "0.00") BigDecimal openingBalance
) {
}
