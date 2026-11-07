package com.sharath.banking.kyc.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sharath.banking.common.ai.ReasoningEngine;
import com.sharath.banking.common.dto.AgentResponse;
import com.sharath.banking.common.event.AgentEvent;
import com.sharath.banking.common.event.Topics;
import com.sharath.banking.common.service.AuditService;
import com.sharath.banking.kyc.dto.KycVerificationDto;
import com.sharath.banking.kyc.entity.Customer;
import com.sharath.banking.kyc.entity.KycStatus;
import com.sharath.banking.kyc.entity.KycVerification;
import com.sharath.banking.kyc.entity.RiskRating;
import com.sharath.banking.kyc.repository.KycVerificationRepository;
import com.sharath.banking.kyc.tools.CustomerDataTool;
import com.sharath.banking.kyc.tools.DocumentValidationTool;
import com.sharath.banking.kyc.tools.SanctionsScreeningTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class KycAgent {

    private static final Logger log = LoggerFactory.getLogger(KycAgent.class);
    private static final String AGENT_NAME = "KYC Agent";

    private final DocumentValidationTool documentValidationTool;
    private final SanctionsScreeningTool sanctionsScreeningTool;
    private final CustomerDataTool customerDataTool;
    private final KycVerificationRepository verificationRepository;
    private final ReasoningEngine reasoningEngine;
    private final AuditService auditService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KycAgent(DocumentValidationTool documentValidationTool,
                    SanctionsScreeningTool sanctionsScreeningTool,
                    CustomerDataTool customerDataTool,
                    KycVerificationRepository verificationRepository,
                    ReasoningEngine reasoningEngine,
                    AuditService auditService,
                    KafkaTemplate<String, Object> kafkaTemplate) {
        this.documentValidationTool = documentValidationTool;
        this.sanctionsScreeningTool = sanctionsScreeningTool;
        this.customerDataTool = customerDataTool;
        this.verificationRepository = verificationRepository;
        this.reasoningEngine = reasoningEngine;
        this.auditService = auditService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public AgentResponse<KycVerificationDto> verifyCustomer(Customer customer) {
        // 1. Document validation tool
        DocumentValidationTool.DocumentValidationResult docResult =
                documentValidationTool.validate(customer.getDocumentType(), customer.getDocumentNumber());

        // 2. Name match: fullName vs document holder name
        double nameMatchScore = computeNameMatchScore(customer.getFullName(), customer.getDocumentHolderName());

        // 3. Sanctions screening tool
        SanctionsScreeningTool.SanctionsScreeningResult sanctionsResult =
                sanctionsScreeningTool.screen(customer.getFullName());

        // 4. Verification score: format 45%, name match 35%, not-expired 20%
        double score = 0.0;
        if (docResult.formatValid()) {
            score += 0.45;
        }
        score += 0.35 * nameMatchScore;
        if (!docResult.expired()) {
            score += 0.20;
        }
        score = Math.round(score * 100.0) / 100.0;

        // 5. Decision
        String decision;
        KycStatus newStatus;
        RiskRating riskRating;
        if (sanctionsResult.hit() || score < 0.5) {
            decision = "REJECTED";
            newStatus = KycStatus.REJECTED;
            riskRating = RiskRating.HIGH;
        } else if (score >= 0.8) {
            decision = "VERIFIED";
            newStatus = KycStatus.VERIFIED;
            riskRating = RiskRating.LOW;
        } else {
            decision = "REVIEW_REQUIRED";
            newStatus = KycStatus.REVIEW_REQUIRED;
            riskRating = RiskRating.MEDIUM;
        }

        // 6. Rule-based explanation listing each check
        String ruleText = buildRuleExplanation(customer, docResult, nameMatchScore, sanctionsResult, score, decision);
        String facts = "Customer: " + customer.getFullName()
                + ", documentType=" + customer.getDocumentType()
                + ", documentNumber=" + customer.getDocumentNumber()
                + ", documentFormatValid=" + docResult.formatValid()
                + ", documentExpired=" + docResult.expired()
                + ", nameMatchScore=" + String.format(Locale.ROOT, "%.2f", nameMatchScore)
                + ", sanctionsHit=" + sanctionsResult.hit()
                + ", verificationScore=" + String.format(Locale.ROOT, "%.2f", score);

        String reasoning = reasoningEngine.explain(AGENT_NAME, "Verify customer identity", facts, ruleText);

        // 7. Persist verification + update customer
        String checksJson = buildChecksJson(docResult, nameMatchScore, sanctionsResult, score);
        KycVerification verification = KycVerification.builder()
                .customerId(customer.getId())
                .documentFormatValid(docResult.formatValid())
                .nameMatchScore(Math.round(nameMatchScore * 100.0) / 100.0)
                .documentExpired(docResult.expired())
                .sanctionsHit(sanctionsResult.hit())
                .verificationScore(score)
                .checksJson(checksJson)
                .decision(decision)
                .reasoning(reasoning)
                .build();
        verification = verificationRepository.save(verification);

        customer.setStatus(newStatus);
        customer.setRiskRating(riskRating);
        customerDataTool.save(customer);

        // 8. Audit
        auditService.record(AGENT_NAME, "VERIFY_IDENTITY", String.valueOf(customer.getId()),
                "Verify KYC for customer " + customer.getFullName() + " (" + customer.getDocumentType()
                        + " " + customer.getDocumentNumber() + ")",
                reasoning, decision);

        // 9. Publish event when verified
        if (newStatus == KycStatus.VERIFIED) {
            AgentEvent event = new AgentEvent(Topics.KYC_VERIFIED, AGENT_NAME,
                    String.valueOf(customer.getId()),
                    "Customer " + customer.getId() + " verified with score "
                            + String.format(Locale.ROOT, "%.2f", score) + ", riskRating=" + riskRating.name(),
                    Instant.now());
            try {
                kafkaTemplate.send(Topics.KYC_VERIFIED, String.valueOf(customer.getId()), event);
            } catch (Exception ex) {
                log.warn("Failed to publish KYC verified event for customer {}: {}", customer.getId(), ex.getMessage());
            }
        }

        double confidence = sanctionsResult.hit() ? 0.99 : Math.min(0.99, 0.55 + score * 0.44);
        return AgentResponse.of(KycVerificationDto.from(verification), reasoning,
                Math.round(confidence * 100.0) / 100.0, AGENT_NAME);
    }

    /** Token-overlap (Jaccard) name similarity, 1.0 for an exact normalized match. */
    public double computeNameMatchScore(String fullName, String documentHolderName) {
        if (fullName == null || documentHolderName == null) {
            return 0.0;
        }
        String a = fullName.trim().toLowerCase(Locale.ROOT);
        String b = documentHolderName.trim().toLowerCase(Locale.ROOT);
        if (a.equals(b)) {
            return 1.0;
        }
        Set<String> tokensA = Stream.of(a.split("\s+")).collect(Collectors.toSet());
        Set<String> tokensB = Stream.of(b.split("\s+")).collect(Collectors.toSet());
        Set<String> intersection = tokensA.stream().filter(tokensB::contains).collect(Collectors.toSet());
        Set<String> union = Stream.concat(tokensA.stream(), tokensB.stream()).collect(Collectors.toSet());
        if (union.isEmpty()) {
            return 0.0;
        }
        double jaccard = (double) intersection.size() / union.size();
        // Partial credit when one name fully contains the other's tokens
        if (intersection.size() == Math.min(tokensA.size(), tokensB.size()) && !tokensA.equals(tokensB)) {
            jaccard = Math.max(jaccard, 0.85);
        }
        return Math.round(jaccard * 100.0) / 100.0;
    }

    private String buildRuleExplanation(Customer customer,
                                        DocumentValidationTool.DocumentValidationResult docResult,
                                        double nameMatchScore,
                                        SanctionsScreeningTool.SanctionsScreeningResult sanctionsResult,
                                        double score, String decision) {
        StringBuilder sb = new StringBuilder();
        sb.append("Document format check: ").append(docResult.formatValid() ? "PASS" : "FAIL")
                .append(" - ").append(docResult.message()).append(". ");
        sb.append("Document expiry check: ").append(docResult.expired() ? "EXPIRED" : "NOT EXPIRED").append(". ");
        sb.append("Name match check: customer name '").append(customer.getFullName())
                .append("' vs document holder '").append(customer.getDocumentHolderName())
                .append("' scored ").append(String.format(Locale.ROOT, "%.2f", nameMatchScore)).append(". ");
        sb.append("Sanctions screening: ").append(sanctionsResult.hit() ? "HIT" : "CLEAR")
                .append(" - ").append(sanctionsResult.message()).append(". ");
        sb.append("Overall verification score: ").append(String.format(Locale.ROOT, "%.2f", score))
                .append(" (VERIFIED requires >= 0.80 with no sanctions hit; 0.50-0.80 requires manual review; below 0.50 or a sanctions hit is rejected). ");
        sb.append("Decision: ").append(decision).append(".");
        return sb.toString();
    }

    private String buildChecksJson(DocumentValidationTool.DocumentValidationResult docResult,
                                   double nameMatchScore,
                                   SanctionsScreeningTool.SanctionsScreeningResult sanctionsResult,
                                   double score) {
        Map<String, Object> checks = new LinkedHashMap<>();
        checks.put("documentFormatValid", docResult.formatValid());
        checks.put("documentExpired", docResult.expired());
        checks.put("documentMessage", docResult.message());
        checks.put("nameMatchScore", Math.round(nameMatchScore * 100.0) / 100.0);
        checks.put("sanctionsHit", sanctionsResult.hit());
        checks.put("sanctionsMessage", sanctionsResult.message());
        checks.put("verificationScore", score);
        try {
            return objectMapper.writeValueAsString(checks);
        } catch (JsonProcessingException e) {
            return "{\"documentFormatValid\":" + docResult.formatValid()
                    + ",\"nameMatchScore\":" + nameMatchScore
                    + ",\"sanctionsHit\":" + sanctionsResult.hit() + "}";
        }
    }
}
