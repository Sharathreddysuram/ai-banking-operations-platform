package com.sharath.banking.decision.dto;
import com.sharath.banking.decision.model.DecisionCase;
import java.time.LocalDateTime;
public record CaseDto(Long id, String caseId, String customerId, String caseType, String kycSummary, String fraudSummary,
        String loanSummary, String supportSummary, String finalRecommendation, double overallRiskScore, String status,
        String reviewedBy, String reviewNotes, String reasoning, LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static CaseDto from(DecisionCase c) {
        return new CaseDto(c.getId(), "CASE-" + c.getId(), c.getCustomerId(), c.getCaseType(), c.getKycSummary(),
                c.getFraudSummary(), c.getLoanSummary(), c.getSupportSummary(), c.getFinalRecommendation(),
                c.getOverallRiskScore(), c.getStatus() != null ? c.getStatus().name() : null, c.getReviewedBy(),
                c.getReviewNotes(), c.getReasoning(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
