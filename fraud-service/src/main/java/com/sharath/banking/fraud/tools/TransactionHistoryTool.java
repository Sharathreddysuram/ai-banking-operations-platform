package com.sharath.banking.fraud.tools;

import com.sharath.banking.fraud.entity.BankTransaction;
import com.sharath.banking.fraud.repository.BankTransactionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/** Tool: aggregates a customer's transaction history for anomaly detection. */
@Component
public class TransactionHistoryTool {

    private final BankTransactionRepository transactionRepository;

    public TransactionHistoryTool(BankTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public record HistorySummary(
            long totalCount,
            long last30DaysCount,
            BigDecimal averageAmount,
            List<String> distinctLocations,
            long nightTransactionCount,
            List<BankTransaction> allTransactions) {
    }

    public HistorySummary summarize(String customerId) {
        List<BankTransaction> all = transactionRepository.findByCustomerIdOrderByTimestampDesc(customerId);
        return summarize(customerId, all);
    }

    /** Summarize over a supplied list (e.g. history excluding the transaction under analysis). */
    public HistorySummary summarize(String customerId, List<BankTransaction> transactions) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long last30 = transactions.stream()
                .filter(t -> t.getTimestamp() != null && t.getTimestamp().isAfter(thirtyDaysAgo))
                .count();
        BigDecimal average = BigDecimal.ZERO;
        if (!transactions.isEmpty()) {
            BigDecimal sum = transactions.stream()
                    .map(BankTransaction::getAmount)
                    .filter(a -> a != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            average = sum.divide(BigDecimal.valueOf(transactions.size()), 2, RoundingMode.HALF_UP);
        }
        List<String> locations = transactions.stream()
                .map(BankTransaction::getLocation)
                .filter(l -> l != null && !l.isBlank())
                .distinct()
                .toList();
        long night = transactions.stream()
                .filter(t -> t.getTimestamp() != null && t.getTimestamp().getHour() < 5)
                .count();
        return new HistorySummary(transactions.size(), last30, average, locations, night, transactions);
    }

    public List<BankTransaction> allForCustomer(String customerId) {
        return transactionRepository.findByCustomerIdOrderByTimestampDesc(customerId);
    }

    public long countInWindow(String customerId, LocalDateTime from, LocalDateTime to) {
        return transactionRepository.findByCustomerIdOrderByTimestampDesc(customerId).stream()
                .filter(t -> t.getTimestamp() != null
                        && !t.getTimestamp().isBefore(from)
                        && !t.getTimestamp().isAfter(to))
                .count();
    }

    public long countInLastHour(String customerId, LocalDateTime reference) {
        return countInWindow(customerId, reference.minusHours(1), reference);
    }
}
