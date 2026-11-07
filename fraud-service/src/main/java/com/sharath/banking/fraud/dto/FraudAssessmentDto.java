package com.sharath.banking.fraud.dto;

import com.sharath.banking.fraud.entity.FraudAssessment;

import java.time.LocalDateTime;
import java.util.List;

public record FraudAssessmentDto(
        Long id,
        Long transactionId,
        String customerId,
        double riskScore,
        String severity,
        List<String> reasons,
        String recommendation,
        String reasoning,
        LocalDateTime createdAt
) {
    public static FraudAssessmentDto from(FraudAssessment a) {
        return new FraudAssessmentDto(a.getId(), a.getTransactionId(), a.getCustomerId(), a.getRiskScore(),
                a.getSeverity(), a.reasonsList(), a.getRecommendation(), a.getReasoning(), a.getCreatedAt());
    }
}
