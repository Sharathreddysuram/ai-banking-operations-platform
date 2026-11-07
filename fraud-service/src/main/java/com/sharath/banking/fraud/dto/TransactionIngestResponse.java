package com.sharath.banking.fraud.dto;

import com.sharath.banking.common.dto.AgentResponse;

public record TransactionIngestResponse(
        BankTransactionDto transaction,
        AgentResponse<FraudAssessmentDto> assessment
) {
}
