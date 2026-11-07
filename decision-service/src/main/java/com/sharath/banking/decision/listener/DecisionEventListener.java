package com.sharath.banking.decision.listener;

import com.sharath.banking.common.event.Topics;
import com.sharath.banking.common.service.AuditService;
import com.sharath.banking.decision.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DecisionEventListener {
    private static final Logger log = LoggerFactory.getLogger(DecisionEventListener.class);
    private final NotificationService notificationService;
    private final AuditService auditService;

    @KafkaListener(topics = Topics.FRAUD_ALERT, groupId = "decision-service")
    public void onFraudAlert(Object payload) {
        log.info("Received fraud alert event: {}", payload);
        notificationService.notify("FRAUD_ALERT", "Fraud alert received", String.valueOf(payload));
        try { auditService.record("Decision Agent", "FRAUD_ALERT_RECEIVED", null, String.valueOf(payload),
                "Fraud alert consumed by decision orchestrator and broadcast to operators.", "Notification broadcast"); } catch (Exception ignored) {}
    }

    @KafkaListener(topics = Topics.KYC_VERIFIED, groupId = "decision-service")
    public void onKycVerified(Object payload) {
        log.info("Received KYC verified event: {}", payload);
        notificationService.notify("KYC_VERIFIED", "KYC verified", String.valueOf(payload));
        try { auditService.record("Decision Agent", "KYC_VERIFIED_RECEIVED", null, String.valueOf(payload),
                "KYC verified event consumed by decision orchestrator.", "Notification broadcast"); } catch (Exception ignored) {}
    }

    @KafkaListener(topics = Topics.LOAN_DECISION, groupId = "decision-service")
    public void onLoanDecision(Object payload) {
        log.info("Received loan decision event: {}", payload);
        notificationService.notify("LOAN_DECISION", "Loan decision received", String.valueOf(payload));
        try { auditService.record("Decision Agent", "LOAN_DECISION_RECEIVED", null, String.valueOf(payload),
                "Loan decision event consumed by decision orchestrator.", "Notification broadcast"); } catch (Exception ignored) {}
    }

    @KafkaListener(topics = Topics.SUPPORT_ESCALATION, groupId = "decision-service")
    public void onSupportEscalation(Object payload) {
        log.info("Received support escalation event: {}", payload);
        notificationService.notify("SUPPORT_ESCALATION", "Support escalation received", String.valueOf(payload));
        try { auditService.record("Decision Agent", "SUPPORT_ESCALATION_RECEIVED", null, String.valueOf(payload),
                "Support escalation event consumed by decision orchestrator.", "Notification broadcast"); } catch (Exception ignored) {}
    }
}
