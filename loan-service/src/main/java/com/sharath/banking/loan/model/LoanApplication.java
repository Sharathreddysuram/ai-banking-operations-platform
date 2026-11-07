package com.sharath.banking.loan.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_application")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String customerId;
    private BigDecimal requestedAmount;
    private int termMonths;
    private String purpose;
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyDebtPayments;
    private BigDecimal annualIncome;
    private int creditScore;
    private String employmentStatus;
    @Enumerated(EnumType.STRING)
    private LoanStatus status;
    private LocalDateTime createdAt;
    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); if (status == null) status = LoanStatus.PENDING; }
}
