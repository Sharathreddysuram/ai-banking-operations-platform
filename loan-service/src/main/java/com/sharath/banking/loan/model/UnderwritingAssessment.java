package com.sharath.banking.loan.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "underwriting_assessment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnderwritingAssessment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long applicationId;
    private double dtiRatio;
    private double riskScore;
    private String riskBand;
    private String recommendation;
    private BigDecimal recommendedAmount;
    @Column(columnDefinition = "TEXT")
    private String reasoning;
    @Column(columnDefinition = "TEXT")
    private String factorsJson;
    private LocalDateTime createdAt;
    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
