package com.sharath.banking.common.event;

import java.time.Instant;

public record AgentEvent(String eventType, String agentName, String customerId, String payload, Instant timestamp) {
}
