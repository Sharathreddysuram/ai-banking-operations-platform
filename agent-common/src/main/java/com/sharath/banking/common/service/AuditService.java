package com.sharath.banking.common.service;

import com.sharath.banking.common.entity.AuditLog;
import com.sharath.banking.common.event.AgentEvent;
import com.sharath.banking.common.event.Topics;
import com.sharath.banking.common.repository.AuditLogRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplateProvider;

    public AuditService(AuditLogRepository auditLogRepository,
                        ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplateProvider) {
        this.auditLogRepository = auditLogRepository;
        this.kafkaTemplateProvider = kafkaTemplateProvider;
    }

    public AuditLog record(String agentName, String action, String customerId, String inputSummary,
                           String reasoning, String outputSummary) {
        AuditLog auditLog = AuditLog.builder()
                .agentName(agentName)
                .action(action)
                .customerId(customerId)
                .inputSummary(inputSummary)
                .reasoning(reasoning)
                .outputSummary(outputSummary)
                .build();
        AuditLog saved = auditLogRepository.save(auditLog);
        try {
            KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
            if (kafkaTemplate != null) {
                AgentEvent event = new AgentEvent(
                        Topics.AGENT_AUDIT,
                        agentName,
                        customerId,
                        outputSummary,
                        Instant.now());
                kafkaTemplate.send(Topics.AGENT_AUDIT, customerId, event);
            }
        } catch (Exception ex) {
            // Best-effort publishing only; audit persistence is the source of truth.
        }
        return saved;
    }
}
