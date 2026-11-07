package com.sharath.banking.fraud.dto;

import com.sharath.banking.fraud.entity.BankTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BankTransactionDto(
        Long id,
        String customerId,
        BigDecimal amount,
        String currency,
        String merchant,
        String category,
        String location,
        String channel,
        LocalDateTime timestamp,
        String status
) {
    public static BankTransactionDto from(BankTransaction t) {
        return new BankTransactionDto(t.getId(), t.getCustomerId(), t.getAmount(), t.getCurrency(),
                t.getMerchant(), t.getCategory(), t.getLocation(), t.getChannel(), t.getTimestamp(), t.getStatus());
    }
}
