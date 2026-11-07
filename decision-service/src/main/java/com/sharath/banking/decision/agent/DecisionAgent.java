package com.sharath.banking.decision.agent;

import com.sharath.banking.common.ai.ReasoningEngine;
import com.sharath.banking.common.event.CaseDecisionEvent;
import com.sharath.banking.common.event.Topics;
import com.sharath.banking.common.service.AuditService;
import com.sharath.banking.decision.client.*;
import com.sharath.banking.decision.model.DecisionCase;
import com.sharath.banking.decision.model.ReviewStatus;
import com.sharath.banking.decision.repository.DecisionCaseRepository;
import com.sharath.banking.decision.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DecisionAgent {
    public static final String AGENT_NAME = "Decision Agent";
    private final DecisionCaseRepository caseRepository;
    private final KycClient kycClient;
    private final FraudClient fraudClient;
    private final LoanClient loanClient;
    private final ReasoningEngine reasoningEngine;
    private final AuditService auditService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final NotificationService notificationService;

    @Transactional
    public DecisionCase createCase(String customerId, String caseType) {
        String type = (caseType == null || caseType.isBlank()) ? "GENERAL" : caseType.toUpperCase();
        ClientResult kyc = kycClient.getCustomerSummary(customerId);
        ClientResult fraud = fraudClient.getFraudSummary(customerId);
        ClientResult loan = loanClient.getLoanSummary(customerId);
        String supportSummary = "Support history checked via event stream: no blocking support escalations assumed unless a support.escalation event was received for this customer.";

        double fraudRisk = fraud.riskOrDefault(50);
        double loanRisk = loan.riskOrDefault(50);
        double kycRisk = kyc.riskOrDefault(50);
        double overall = Math.round((fraudRisk * 0.5 + loanRisk * 0.3 + kycRisk * 0.2) * 10.0) / 10.0;

        String recommendation;
        if (fraudRisk >= 80) recommendation = "DECLINE - high fraud risk";
        else if (overall <= 35) recommendation = "APPROVE - low composite risk";
        else if (overall >= 65) recommendation = "ESCALATE TO HUMAN REVIEW - high composite risk";
        else recommendation = "ESCALATE TO HUMAN REVIEW - moderate composite risk";

        String facts = "customerId=" + customerId + ", caseType=" + type + ", fraudRisk=" + fraudRisk
                + ", loanRisk=" + loanRisk + ", kycRisk=" + kycRisk + ", overallRisk=" + overall
                + ", kycAvailable=" + kyc.available() + ", fraudAvailable=" + fraud.available() + ", loanAvailable=" + loan.available();
        String baseReasoning = "Composite risk " + overall + " = fraud " + fraudRisk + " x0.5 + loan " + loanRisk
                + " x0.3 + kyc " + kycRisk + " x0.2. Recommendation: " + recommendation
                + ". Case always starts PENDING_REVIEW for human-in-the-loop sign-off.";
        String reasoning = reasoningEngine.explain(AGENT_NAME, "AGGREGATE_DECISION", facts, baseReasoning);

        DecisionCase saved = caseRepository.save(DecisionCase.builder()
                .customerId(customerId).caseType(type)
                .kycSummary(kyc.summary()).fraudSummary(fraud.summary()).loanSummary(loan.summary())
                .supportSummary(supportSummary).finalRecommendation(recommendation)
                .overallRiskScore(overall).status(ReviewStatus.PENDING_REVIEW).reasoning(reasoning).build());
        auditService.record(AGENT_NAME, "AGGREGATE_DECISION", customerId,
                "caseType=" + type + " customerId=" + customerId, reasoning,
                recommendation + " overallRisk=" + overall + " caseId=CASE-" + saved.getId());
        try {
            kafkaTemplate.send(Topics.CASE_DECISION, customerId,
                    new CaseDecisionEvent("CASE-" + saved.getId(), customerId, recommendation,
                            "Overall risk " + overall + " - " + recommendation, Instant.now()));
        } catch (Exception ignored) {}
        notificationService.notify("CASE_CREATED", "New decision case CASE-" + saved.getId(),
                recommendation + " for customer " + customerId + " (risk " + overall + ")");
        return saved;
    }
}
