package com.sharath.banking.loan.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "financial_document")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long applicationId;
    private String documentType;
    @Column(columnDefinition = "TEXT")
    private String extractedData;
    private BigDecimal verifiedMonthlyIncome;
    private LocalDateTime createdAt;
    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
