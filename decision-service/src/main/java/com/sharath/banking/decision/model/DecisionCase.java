package com.sharath.banking.decision.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "decision_case")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DecisionCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String customerId;
    private String caseType;
    @Column(columnDefinition = "TEXT")
    private String kycSummary;
    @Column(columnDefinition = "TEXT")
    private String fraudSummary;
    @Column(columnDefinition = "TEXT")
    private String loanSummary;
    @Column(columnDefinition = "TEXT")
    private String supportSummary;
    private String finalRecommendation;
    private double overallRiskScore;
    @Enumerated(EnumType.STRING)
    private ReviewStatus status;
    private String reviewedBy;
    @Column(columnDefinition = "TEXT")
    private String reviewNotes;
    @Column(columnDefinition = "TEXT")
    private String reasoning;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); if (status == null) status = ReviewStatus.PENDING_REVIEW; }
    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}
