package com.sharath.banking.fraud.service;

import com.sharath.banking.common.dto.AgentResponse;
import com.sharath.banking.fraud.agent.FraudAgent;
import com.sharath.banking.fraud.dto.BankTransactionDto;
import com.sharath.banking.fraud.dto.CreateTransactionRequest;
import com.sharath.banking.fraud.dto.FraudAssessmentDto;
import com.sharath.banking.fraud.dto.TransactionHistorySummaryDto;
import com.sharath.banking.fraud.dto.TransactionIngestResponse;
import com.sharath.banking.fraud.entity.BankTransaction;
import com.sharath.banking.fraud.repository.BankTransactionRepository;
import com.sharath.banking.fraud.repository.FraudAssessmentRepository;
import com.sharath.banking.fraud.tools.TransactionHistoryTool;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class FraudService {

    private final BankTransactionRepository transactionRepository;
    private final FraudAssessmentRepository assessmentRepository;
    private final TransactionHistoryTool historyTool;
    private final FraudAgent fraudAgent;

    public FraudService(BankTransactionRepository transactionRepository,
                        FraudAssessmentRepository assessmentRepository,
                        TransactionHistoryTool historyTool,
                        FraudAgent fraudAgent) {
        this.transactionRepository = transactionRepository;
        this.assessmentRepository = assessmentRepository;
        this.historyTool = historyTool;
        this.fraudAgent = fraudAgent;
    }

    @Transactional
    @CacheEvict(value = "transactionHistory", key = "#request.customerId()")
    public TransactionIngestResponse ingestAndAnalyze(CreateTransactionRequest request) {
        BankTransaction transaction = BankTransaction.builder()
                .customerId(request.customerId())
                .amount(request.amount())
                .currency(request.currency() != null ? request.currency() : "USD")
                .merchant(request.merchant())
                .category(request.category())
                .location(request.location())
                .channel(request.channel())
                .timestamp(request.timestamp())
                .status("PENDING")
                .build();
        transaction = transactionRepository.save(transaction);
        AgentResponse<FraudAssessmentDto> assessment = fraudAgent.analyzeTransaction(transaction);
        BankTransaction refreshed = transactionRepository.findById(transaction.getId()).orElse(transaction);
        return new TransactionIngestResponse(BankTransactionDto.from(refreshed), assessment);
    }

    @Transactional(readOnly = true)
    public List<BankTransactionDto> listTransactions(String customerId) {
        List<BankTransaction> transactions = (customerId == null || customerId.isBlank())
                ? transactionRepository.findAll()
                : transactionRepository.findByCustomerIdOrderByTimestampDesc(customerId);
        return transactions.stream().map(BankTransactionDto::from).toList();
    }

    @Transactional(readOnly = true)
    public BankTransactionDto getTransaction(Long id) {
        return BankTransactionDto.from(findTransaction(id));
    }

    @Transactional
    @CacheEvict(value = "transactionHistory", allEntries = true)
    public AgentResponse<FraudAssessmentDto> analyze(Long id) {
        BankTransaction transaction = findTransaction(id);
        return fraudAgent.analyzeTransaction(transaction);
    }

    @Transactional(readOnly = true)
    public List<FraudAssessmentDto> listAssessments(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return assessmentRepository.findAll().stream().map(FraudAssessmentDto::from).toList();
        }
        return assessmentRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(FraudAssessmentDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<FraudAssessmentDto> listAlerts() {
        return assessmentRepository.findBySeverityInOrderByCreatedAtDesc(List.of("HIGH", "CRITICAL"))
                .stream().map(FraudAssessmentDto::from).toList();
    }

    @Cacheable(value = "transactionHistory", key = "#customerId")
    @Transactional(readOnly = true)
    public TransactionHistorySummaryDto historySummary(String customerId) {
        TransactionHistoryTool.HistorySummary summary = historyTool.summarize(customerId);
        return new TransactionHistorySummaryDto(customerId, summary.totalCount(), summary.last30DaysCount(),
                summary.averageAmount(), summary.distinctLocations(), summary.nightTransactionCount());
    }

    private BankTransaction findTransaction(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Transaction not found: " + id));
    }
}
