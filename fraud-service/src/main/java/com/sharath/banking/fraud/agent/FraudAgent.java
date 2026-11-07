package com.sharath.banking.fraud.agent;

import com.sharath.banking.common.ai.ReasoningEngine;
import com.sharath.banking.common.dto.AgentResponse;
import com.sharath.banking.common.event.FraudAlertEvent;
import com.sharath.banking.common.event.Topics;
import com.sharath.banking.common.service.AuditService;
import com.sharath.banking.fraud.dto.FraudAssessmentDto;
import com.sharath.banking.fraud.entity.BankTransaction;
import com.sharath.banking.fraud.entity.FraudAssessment;
import com.sharath.banking.fraud.repository.BankTransactionRepository;
import com.sharath.banking.fraud.repository.FraudAssessmentRepository;
import com.sharath.banking.fraud.tools.CustomerDataTool;
import com.sharath.banking.fraud.tools.GeoVelocityTool;
import com.sharath.banking.fraud.tools.TransactionHistoryTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class FraudAgent {

    private static final Logger log = LoggerFactory.getLogger(FraudAgent.class);
    private static final String AGENT_NAME = "Fraud Detection Agent";
    private static final BigDecimal LARGE_AMOUNT_THRESHOLD = new BigDecimal("10000");

    private final TransactionHistoryTool historyTool;
    private final GeoVelocityTool geoVelocityTool;
    private final CustomerDataTool customerDataTool;
    private final FraudAssessmentRepository assessmentRepository;
    private final BankTransactionRepository transactionRepository;
    private final ReasoningEngine reasoningEngine;
    private final AuditService auditService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public FraudAgent(TransactionHistoryTool historyTool,
                      GeoVelocityTool geoVelocityTool,
                      CustomerDataTool customerDataTool,
                      FraudAssessmentRepository assessmentRepository,
                      BankTransactionRepository transactionRepository,
                      ReasoningEngine reasoningEngine,
                      AuditService auditService,
                      KafkaTemplate<String, Object> kafkaTemplate) {
        this.historyTool = historyTool;
        this.geoVelocityTool = geoVelocityTool;
        this.customerDataTool = customerDataTool;
        this.assessmentRepository = assessmentRepository;
        this.transactionRepository = transactionRepository;
        this.reasoningEngine = reasoningEngine;
        this.auditService = auditService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public AgentResponse<FraudAssessmentDto> analyzeTransaction(BankTransaction transaction) {
        LocalDateTime txnTime = transaction.getTimestamp() != null ? transaction.getTimestamp() : LocalDateTime.now();

        // Baseline history excludes the transaction under analysis
        List<BankTransaction> allForCustomer = historyTool.allForCustomer(transaction.getCustomerId());
        List<BankTransaction> history = allForCustomer.stream()
                .filter(t -> transaction.getId() == null || !transaction.getId().equals(t.getId()))
                .toList();
        TransactionHistoryTool.HistorySummary summary = historyTool.summarize(transaction.getCustomerId(), history);

        double score = 0.0;
        List<String> reasons = new ArrayList<>();

        // Rule 1: amount > 3x customer average (+35)
        if (summary.averageAmount() != null && summary.averageAmount().compareTo(BigDecimal.ZERO) > 0
                && transaction.getAmount() != null) {
            BigDecimal threeX = summary.averageAmount().multiply(new BigDecimal("3"));
            if (transaction.getAmount().compareTo(threeX) > 0) {
                score += 35;
                reasons.add("Transaction amount " + transaction.getAmount()
                        + " is more than 3x the customer's average transaction amount of "
                        + summary.averageAmount() + " (+35)");
            }
        }

        // Rule 2: amount > 10000 (+20)
        if (transaction.getAmount() != null && transaction.getAmount().compareTo(LARGE_AMOUNT_THRESHOLD) > 0) {
            score += 20;
            reasons.add("Transaction amount " + transaction.getAmount() + " exceeds the 10000 large-transaction threshold (+20)");
        }

        // Rule 3: night-time transaction 00:00-05:00 (+15)
        int hour = txnTime.getHour();
        if (hour >= 0 && hour < 5) {
            score += 15;
            reasons.add("Transaction occurred at night (" + String.format(Locale.ROOT, "%02d:%02d", hour, txnTime.getMinute())
                    + "), within the 00:00-05:00 high-risk window (+15)");
        }

        // Rule 4: new location not in history (+20)
        boolean hasHistory = !history.isEmpty();
        boolean knownLocation = transaction.getLocation() != null && summary.distinctLocations().stream()
                .anyMatch(l -> l.equalsIgnoreCase(transaction.getLocation().trim()));
        if (hasHistory && transaction.getLocation() != null && !knownLocation) {
            score += 20;
            reasons.add("Transaction location '" + transaction.getLocation()
                    + "' has never been seen in this customer's history (+20)");
        }

        // Rule 5: velocity - more than 5 transactions in 1 hour (+25), including current
        long inLastHour = allForCustomer.stream()
                .filter(t -> t.getTimestamp() != null
                        && !t.getTimestamp().isBefore(txnTime.minusHours(1))
                        && !t.getTimestamp().isAfter(txnTime))
                .count();
        // allForCustomer includes the current transaction when it was persisted first; if not, count it
        if (transaction.getId() == null || allForCustomer.stream().noneMatch(t -> transaction.getId().equals(t.getId()))) {
            inLastHour += 1;
        }
        if (inLastHour > 5) {
            score += 25;
            reasons.add("Velocity anomaly: " + inLastHour + " transactions within one hour for this customer (+25)");
        }

        // Rule 6: high-risk category (+15)
        String category = transaction.getCategory() != null ? transaction.getCategory().trim().toUpperCase(Locale.ROOT) : "";
        if (category.equals("CRYPTO") || category.equals("GAMBLING")) {
            score += 15;
            reasons.add("High-risk merchant category '" + transaction.getCategory() + "' (+15)");
        }

        // Rule 7: foreign location vs home country (+15)
        GeoVelocityTool.GeoVelocityResult geo = geoVelocityTool.check(transaction, history);
        if (geoVelocityTool.isForeignLocation(transaction, history)) {
            score += 15;
            reasons.add("Transaction location '" + transaction.getLocation() + "' is in a foreign country ("
                    + geoVelocityTool.countryOf(transaction.getLocation()) + ") versus the customer's home country ("
                    + geoVelocityTool.homeCountry(history) + ") (+15)");
        }
        if (geo.rapidLocationChange()) {
            // Explainable context for the new-location / foreign rules (no extra points beyond rules 4 and 7)
            reasons.add("Geo-velocity: " + geo.message());
        }

        score = Math.min(100.0, score);

        String severity;
        if (score >= 70) {
            severity = "CRITICAL";
        } else if (score >= 40) {
            severity = "HIGH";
        } else if (score >= 20) {
            severity = "MEDIUM";
        } else {
            severity = "LOW";
        }

        String recommendation;
        if (score >= 70) {
            recommendation = "BLOCK";
        } else if (score >= 40) {
            recommendation = "REVIEW";
        } else {
            recommendation = "APPROVE";
        }

        String ruleText = reasons.isEmpty()
                ? "No anomaly rules were triggered. Amount, time, location, velocity and category are all "
                        + "consistent with the customer's history. Risk score 0, recommendation APPROVE."
                : "Triggered rules: " + String.join("; ", reasons)
                        + ". Total risk score " + (int) score + "/100, severity " + severity
                        + ", recommendation " + recommendation + ".";
        String facts = "Transaction: customerId=" + transaction.getCustomerId()
                + ", amount=" + transaction.getAmount() + " " + transaction.getCurrency()
                + ", merchant=" + transaction.getMerchant() + ", category=" + transaction.getCategory()
                + ", location=" + transaction.getLocation() + ", channel=" + transaction.getChannel()
                + ", timestamp=" + txnTime
                + ", historyCount=" + summary.totalCount()
                + ", historyAverage=" + summary.averageAmount()
                + ", knownLocations=" + summary.distinctLocations()
                + ", profile=" + customerDataTool.fetchCustomerProfile(transaction.getCustomerId());

        String reasoning = reasoningEngine.explain(AGENT_NAME, "Analyze transaction for fraud", facts, ruleText);

        FraudAssessment assessment = FraudAssessment.builder()
                .transactionId(transaction.getId())
                .customerId(transaction.getCustomerId())
                .riskScore(score)
                .severity(severity)
                .reasonsJson(FraudAssessment.joinReasons(reasons))
                .recommendation(recommendation)
                .reasoning(reasoning)
                .build();
        assessment = assessmentRepository.save(assessment);

        // Reflect the recommendation on the transaction status
        if (transaction.getId() != null) {
            transaction.setStatus(switch (recommendation) {
                case "BLOCK" -> "BLOCKED";
                case "REVIEW" -> "UNDER_REVIEW";
                default -> "APPROVED";
            });
            transactionRepository.save(transaction);
        }

        auditService.record(AGENT_NAME, "ANALYZE_TRANSACTION", transaction.getCustomerId(),
                "Analyze transaction " + transaction.getId() + ": " + transaction.getAmount()
                        + " " + transaction.getCurrency() + " at " + transaction.getMerchant()
                        + " (" + transaction.getLocation() + ")",
                reasoning,
                "riskScore=" + (int) score + ", severity=" + severity + ", recommendation=" + recommendation);

        if (severity.equals("HIGH") || severity.equals("CRITICAL")) {
            FraudAlertEvent alert = new FraudAlertEvent(
                    transaction.getCustomerId(),
                    String.valueOf(transaction.getId()),
                    score,
                    severity,
                    List.copyOf(reasons),
                    Instant.now());
            try {
                kafkaTemplate.send(Topics.FRAUD_ALERT, transaction.getCustomerId(), alert);
            } catch (Exception ex) {
                log.warn("Failed to publish fraud alert for transaction {}: {}", transaction.getId(), ex.getMessage());
            }
        }

        double confidence = Math.round(Math.min(0.99, 0.60 + (score / 100.0) * 0.39) * 100.0) / 100.0;
        return AgentResponse.of(FraudAssessmentDto.from(assessment), reasoning, confidence, AGENT_NAME);
    }
}
