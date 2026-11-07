package com.sharath.banking.common.event;

import java.time.Instant;

public record CaseDecisionEvent(String caseId, String customerId, String decision, String summary, Instant timestamp) {
}
