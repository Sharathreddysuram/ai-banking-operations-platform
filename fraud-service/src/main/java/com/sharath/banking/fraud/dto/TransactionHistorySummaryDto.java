package com.sharath.banking.fraud.dto;

import java.math.BigDecimal;
import java.util.List;

public record TransactionHistorySummaryDto(
        String customerId,
        long totalTransactions,
        long last30DaysCount,
        BigDecimal averageAmount,
        List<String> distinctLocations,
        long nightTransactionCount
) {
}
