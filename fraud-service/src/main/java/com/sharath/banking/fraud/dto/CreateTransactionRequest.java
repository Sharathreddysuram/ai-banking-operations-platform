package com.sharath.banking.fraud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateTransactionRequest(
        @NotBlank String customerId,
        @NotNull BigDecimal amount,
        String currency,
        String merchant,
        String category,
        String location,
        String channel,
        LocalDateTime timestamp
) {
}
