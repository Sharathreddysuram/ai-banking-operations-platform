package com.sharath.banking.loan.controller;

import com.sharath.banking.common.dto.AgentResponse;
import com.sharath.banking.loan.agent.LoanUnderwritingAgent;
import com.sharath.banking.loan.dto.*;
import com.sharath.banking.loan.model.*;
import com.sharath.banking.loan.repository.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/loan")
@RequiredArgsConstructor
public class LoanController {
    private final LoanApplicationRepository applicationRepository;
    private final UnderwritingAssessmentRepository assessmentRepository;
    private final FinancialDocumentRepository documentRepository;
    private final LoanUnderwritingAgent agent;

    @PostMapping("/applications")
    public ResponseEntity<ApplicationDto> create(@Valid @RequestBody ApplicationRequest req) {
        BigDecimal annual = req.annualIncome() != null ? req.annualIncome()
                : (req.monthlyIncome() != null ? req.monthlyIncome().multiply(BigDecimal.valueOf(12)) : BigDecimal.ZERO);
        BigDecimal debt = req.monthlyDebtPayments() != null ? req.monthlyDebtPayments() : BigDecimal.ZERO;
        LoanApplication saved = applicationRepository.save(LoanApplication.builder()
                .customerId(req.customerId()).requestedAmount(req.requestedAmount()).termMonths(req.termMonths())
                .purpose(req.purpose()).monthlyIncome(req.monthlyIncome()).monthlyDebtPayments(debt)
                .annualIncome(annual).creditScore(req.creditScore())
                .employmentStatus(req.employmentStatus() != null ? req.employmentStatus() : "FULL_TIME")
                .status(LoanStatus.PENDING).build());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApplicationDto.from(saved));
    }

    @GetMapping("/applications")
    public List<ApplicationDto> byCustomer(@RequestParam String customerId) {
        return applicationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream().map(ApplicationDto::from).toList();
    }

    @GetMapping("/applications/{id}")
    public ResponseEntity<ApplicationDto> get(@PathVariable Long id) {
        return applicationRepository.findById(id).map(a -> ResponseEntity.ok(ApplicationDto.from(a)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/applications/{id}/documents")
    public ResponseEntity<DocumentDto> addDocument(@PathVariable Long id, @Valid @RequestBody DocumentRequest req) {
        if (!applicationRepository.existsById(id)) return ResponseEntity.notFound().build();
        FinancialDocument saved = documentRepository.save(FinancialDocument.builder()
                .applicationId(id).documentType(req.documentType().toUpperCase())
                .extractedData(req.extractedData()).verifiedMonthlyIncome(req.verifiedMonthlyIncome()).build());
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentDto.from(saved));
    }

    @PostMapping("/applications/{id}/underwrite")
    public ResponseEntity<AgentResponse<AssessmentDto>> underwrite(@PathVariable Long id) {
        if (!applicationRepository.existsById(id)) return ResponseEntity.notFound().build();
        UnderwritingAssessment a = agent.assess(id);
        double confidence = a.getRecommendation().equals("APPROVE") || a.getRecommendation().equals("REJECT") ? 0.90 : 0.70;
        return ResponseEntity.ok(AgentResponse.of(AssessmentDto.from(a), a.getReasoning(), confidence, LoanUnderwritingAgent.AGENT_NAME));
    }

    @GetMapping("/applications/{id}/assessment")
    public ResponseEntity<AssessmentDto> assessment(@PathVariable Long id) {
        return assessmentRepository.findTopByApplicationIdOrderByCreatedAtDesc(id)
                .map(a -> ResponseEntity.ok(AssessmentDto.from(a))).orElse(ResponseEntity.notFound().build());
    }
}
