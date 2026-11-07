package com.sharath.banking.decision.controller;

import com.sharath.banking.common.ai.ReasoningEngine;
import com.sharath.banking.common.entity.AuditLog;
import com.sharath.banking.common.repository.AuditLogRepository;
import com.sharath.banking.common.service.AuditService;
import com.sharath.banking.decision.agent.DecisionAgent;
import com.sharath.banking.decision.dto.*;
import com.sharath.banking.decision.model.DecisionCase;
import com.sharath.banking.decision.model.ReviewStatus;
import com.sharath.banking.decision.repository.DecisionCaseRepository;
import com.sharath.banking.decision.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/decision")
@RequiredArgsConstructor
public class DecisionController {
    private final DecisionAgent agent;
    private final DecisionCaseRepository caseRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final ReasoningEngine reasoningEngine;
    private final NotificationService notificationService;

    @PostMapping("/cases")
    public ResponseEntity<CaseDto> create(@Valid @RequestBody CreateCaseRequest req) {
        DecisionCase created = agent.createCase(req.customerId(), req.caseType());
        return ResponseEntity.status(HttpStatus.CREATED).body(CaseDto.from(created));
    }

    @GetMapping("/cases")
    public List<CaseDto> cases(@RequestParam(required = false) String customerId) {
        List<DecisionCase> list = customerId != null && !customerId.isBlank()
                ? caseRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                : caseRepository.findAllByOrderByCreatedAtDesc();
        return list.stream().map(CaseDto::from).toList();
    }

    @GetMapping("/cases/{id}")
    public ResponseEntity<CaseDto> get(@PathVariable Long id) {
        return caseRepository.findById(id).map(c -> ResponseEntity.ok(CaseDto.from(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/cases/{id}/review")
    public ResponseEntity<CaseDto> review(@PathVariable Long id, @Valid @RequestBody ReviewRequest req) {
        return caseRepository.findById(id).map(c -> {
            ReviewStatus status;
            try { status = ReviewStatus.valueOf(req.decision().trim().toUpperCase()); }
            catch (Exception e) { return ResponseEntity.badRequest().<CaseDto>build(); }
            if (status == ReviewStatus.PENDING_REVIEW) return ResponseEntity.badRequest().<CaseDto>build();
            c.setStatus(status);
            c.setReviewedBy(req.reviewedBy());
            c.setReviewNotes(req.notes());
            DecisionCase saved = caseRepository.save(c);
            auditService.record("Decision Agent", "HUMAN_REVIEW", saved.getCustomerId(),
                    "caseId=CASE-" + saved.getId() + " decision=" + status,
                    "Human reviewer " + req.reviewedBy() + " decided " + status + ". Notes: " + req.notes(),
                    "Case CASE-" + saved.getId() + " " + status);
            notificationService.notify("CASE_REVIEWED", "Case CASE-" + saved.getId() + " " + status,
                    "Reviewed by " + req.reviewedBy() + ": " + req.notes());
            return ResponseEntity.ok(CaseDto.from(saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/audit")
    public List<AuditLog> audit() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc();
    }

    @GetMapping("/audit/customer/{customerId}")
    public List<AuditLog> auditForCustomer(@PathVariable String customerId) {
        return auditLogRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @GetMapping("/agents/status")
    public List<AgentStatusDto> agentsStatus() {
        String mode = reasoningEngine.mode();
        return List.of(
            new AgentStatusDto("KYC Verification Agent", mode, "ACTIVE", "Verifies customer identity documents and onboarding KYC checks."),
            new AgentStatusDto("Fraud Detection Agent", mode, "ACTIVE", "Scores transactions for fraud risk and raises fraud alerts."),
            new AgentStatusDto("Loan Underwriting Agent", mode, "ACTIVE", "Assesses income, debt and credit risk to underwrite loan applications."),
            new AgentStatusDto("Customer Support Agent", mode, "ACTIVE", "Answers customer queries from the knowledge base and escalates sensitive cases."),
            new AgentStatusDto("Decision Agent", mode, "ACTIVE", "Aggregates KYC, fraud, loan and support signals into human-reviewed decision cases.")
        );
    }
}
