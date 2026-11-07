package com.sharath.banking.fraud.controller;

import com.sharath.banking.common.dto.AgentResponse;
import com.sharath.banking.fraud.dto.BankTransactionDto;
import com.sharath.banking.fraud.dto.CreateTransactionRequest;
import com.sharath.banking.fraud.dto.FraudAssessmentDto;
import com.sharath.banking.fraud.dto.TransactionHistorySummaryDto;
import com.sharath.banking.fraud.dto.TransactionIngestResponse;
import com.sharath.banking.fraud.service.FraudService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/fraud")
public class FraudController {

    private final FraudService fraudService;

    public FraudController(FraudService fraudService) {
        this.fraudService = fraudService;
    }

    @PostMapping("/transactions")
    public ResponseEntity<TransactionIngestResponse> ingestTransaction(
            @Valid @RequestBody CreateTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fraudService.ingestAndAnalyze(request));
    }

    @GetMapping("/transactions")
    public List<BankTransactionDto> listTransactions(@RequestParam(required = false) String customerId) {
        return fraudService.listTransactions(customerId);
    }

    @GetMapping("/transactions/{id}")
    public BankTransactionDto getTransaction(@PathVariable Long id) {
        return fraudService.getTransaction(id);
    }

    @PostMapping("/transactions/{id}/analyze")
    public AgentResponse<FraudAssessmentDto> analyze(@PathVariable Long id) {
        return fraudService.analyze(id);
    }

    @GetMapping("/transactions/summary")
    public TransactionHistorySummaryDto historySummary(@RequestParam String customerId) {
        return fraudService.historySummary(customerId);
    }

    @GetMapping("/assessments")
    public List<FraudAssessmentDto> listAssessments(@RequestParam(required = false) String customerId) {
        return fraudService.listAssessments(customerId);
    }

    @GetMapping("/alerts")
    public List<FraudAssessmentDto> listAlerts() {
        return fraudService.listAlerts();
    }
}
