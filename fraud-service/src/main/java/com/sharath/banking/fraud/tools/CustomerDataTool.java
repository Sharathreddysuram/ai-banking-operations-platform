package com.sharath.banking.fraud.tools;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** Tool: stub customer profile fetch, derived from transaction history. */
@Component
public class CustomerDataTool {

    private final TransactionHistoryTool historyTool;

    public CustomerDataTool(TransactionHistoryTool historyTool) {
        this.historyTool = historyTool;
    }

    public Map<String, Object> fetchCustomerProfile(String customerId) {
        TransactionHistoryTool.HistorySummary summary = historyTool.summarize(customerId);
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("customerId", customerId);
        profile.put("knownCustomer", summary.totalCount() > 0);
        profile.put("totalTransactions", summary.totalCount());
        profile.put("averageAmount", summary.averageAmount());
        profile.put("distinctLocations", summary.distinctLocations());
        profile.put("nightTransactionCount", summary.nightTransactionCount());
        return profile;
    }
}
