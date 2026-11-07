package com.sharath.banking.common.event;

import java.time.Instant;
import java.util.List;

public record FraudAlertEvent(String customerId, String transactionId, double riskScore, String severity,
                              List<String> reasons, Instant timestamp) {
}
