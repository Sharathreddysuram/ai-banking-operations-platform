package com.sharath.banking.loan.agent;

import com.sharath.banking.common.ai.ReasoningEngine;
import com.sharath.banking.common.event.AgentEvent;
import com.sharath.banking.common.event.Topics;
import com.sharath.banking.common.service.AuditService;
import com.sharath.banking.loan.model.*;
import com.sharath.banking.loan.repository.*;
import com.sharath.banking.loan.tools.*;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanUnderwritingAgent {
    public static final String AGENT_NAME = "Loan Underwriting Agent";
    private final LoanApplicationRepository applicationRepository;
    private final UnderwritingAssessmentRepository assessmentRepository;
    private final FinancialDocumentRepository documentRepository;
    private final IncomeAssessmentTool incomeTool;
    private final DebtAnalysisTool debtTool;
    private final CreditRiskTool creditTool;
    private final ReasoningEngine reasoningEngine;
    private final AuditService auditService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public UnderwritingAssessment assess(Long applicationId) {
        LoanApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found: " + applicationId));
        return assess(app);
    }

    @Transactional
    public UnderwritingAssessment assess(LoanApplication application) {
        List<FinancialDocument> docs = documentRepository.findByApplicationId(application.getId());
        var income = incomeTool.assess(application, docs);
        BigDecimal effectiveIncome = income.verifiedMonthlyIncome();
        var debt = debtTool.analyse(application.getMonthlyDebtPayments(), effectiveIncome,
                application.getRequestedAmount(), application.getTermMonths());
        double dti = debt.dtiRatio();
        double creditRisk = creditTool.scoreToRisk(application.getCreditScore());
        double dtiRisk = Math.min(100, dti * 150);
        BigDecimal annual = application.getAnnualIncome() != null && application.getAnnualIncome().doubleValue() > 0
                ? application.getAnnualIncome() : effectiveIncome.multiply(BigDecimal.valueOf(12));
        double lti = annual.doubleValue() > 0 ? application.getRequestedAmount().doubleValue() / annual.doubleValue() : 1.0;
        double ltiRisk = Math.min(100, lti * 100);
        double employmentRisk = employmentRisk(application.getEmploymentStatus());
        double risk = creditRisk * 0.50 + dtiRisk * 0.30 + ltiRisk * 0.10 + employmentRisk * 0.10;
        if (dti > 0.43) risk += 10;
        risk = Math.max(0, Math.min(100, Math.round(risk * 10.0) / 10.0));

        String recommendation;
        BigDecimal recommended;
        LoanStatus newStatus;
        if (risk < 35 && dti <= 0.36) {
            recommendation = "APPROVE"; recommended = application.getRequestedAmount(); newStatus = LoanStatus.APPROVED;
        } else if (risk < 55) {
            recommendation = "APPROVE_WITH_CONDITIONS";
            recommended = application.getRequestedAmount().multiply(new BigDecimal("0.70")).setScale(2, RoundingMode.HALF_UP);
            newStatus = LoanStatus.APPROVED;
        } else if (risk < 75) {
            recommendation = "MANUAL_REVIEW";
            recommended = application.getRequestedAmount().multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP);
            newStatus = LoanStatus.MANUAL_REVIEW;
        } else {
            recommendation = "REJECT"; recommended = BigDecimal.ZERO; newStatus = LoanStatus.REJECTED;
        }
        String riskBand = risk < 25 ? "LOW" : risk < 50 ? "MODERATE" : risk < 75 ? "HIGH" : "VERY_HIGH";

        List<String> factors = new ArrayList<>();
        factors.add("Credit score " + application.getCreditScore() + " (" + creditTool.band(application.getCreditScore()) + ") contributes risk " + String.format("%.1f", creditRisk));
        factors.add("DTI ratio " + String.format("%.3f", dti) + " including estimated payment $" + debt.estimatedMonthlyPayment() + " (threshold 0.36 preferred, 0.43 maximum)");
        factors.add("Verified monthly income $" + effectiveIncome + " from " + income.source() + (income.verified() ? " (document verified)" : " (declared, no document)"));
        factors.add("Loan-to-income ratio " + String.format("%.3f", lti) + " on annual income $" + annual);
        factors.add("Employment status " + application.getEmploymentStatus() + " stability risk " + String.format("%.0f", employmentRisk));
        factors.add("Composite risk score " + risk + " in band " + riskBand + " leads to " + recommendation);

        String facts = "customerId=" + application.getCustomerId() + ", creditScore=" + application.getCreditScore()
                + ", dti=" + String.format("%.3f", dti) + ", riskScore=" + risk + ", requested=" + application.getRequestedAmount()
                + ", incomeSource=" + income.source();
        String baseReasoning = "Recommendation " + recommendation + " with risk " + risk + " (" + riskBand + "). " + String.join("; ", factors);
        String reasoning = reasoningEngine.explain(AGENT_NAME, "UNDERWRITE_LOAN", facts, baseReasoning);

        UnderwritingAssessment assessment = assessmentRepository.save(UnderwritingAssessment.builder()
                .applicationId(application.getId()).dtiRatio(Math.round(dti * 10000.0) / 10000.0).riskScore(risk)
                .riskBand(riskBand).recommendation(recommendation).recommendedAmount(recommended)
                .reasoning(reasoning).factorsJson(String.join("\n", factors)).build());
        application.setStatus(newStatus);
        applicationRepository.save(application);
        auditService.record(AGENT_NAME, "UNDERWRITE_LOAN", application.getCustomerId(),
                "applicationId=" + application.getId() + " amount=" + application.getRequestedAmount(),
                reasoning, recommendation + " risk=" + risk + " dti=" + String.format("%.3f", dti));
        try {
            kafkaTemplate.send(Topics.LOAN_DECISION, application.getCustomerId(),
                    new AgentEvent(Topics.LOAN_DECISION, AGENT_NAME, application.getCustomerId(), recommendation, Instant.now()));
        } catch (Exception ignored) {
            // Kafka is best-effort; the assessment and audit trail are already persisted.
        }
        return assessment;
    }

    private double employmentRisk(String status) {
        if (status == null) return 50;
        return switch (status.trim().toUpperCase()) {
            case "FULL_TIME", "FULL-TIME", "EMPLOYED" -> 0;
            case "CONTRACT" -> 30;
            case "PART_TIME", "PART-TIME" -> 40;
            case "SELF_EMPLOYED", "SELF-EMPLOYED" -> 50;
            case "RETIRED" -> 60;
            case "UNEMPLOYED" -> 100;
            default -> 50;
        };
    }
}
