package com.sharath.banking.fraud.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "fraud_assessment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long transactionId;

    @Column(nullable = false)
    private String customerId;

    private double riskScore;

    /** LOW / MEDIUM / HIGH / CRITICAL */
    @Column(nullable = false)
    private String severity;

    /** Reasons stored as TEXT, joined with a newline separator. */
    @Column(columnDefinition = "TEXT")
    private String reasonsJson;

    /** APPROVE / REVIEW / BLOCK */
    @Column(nullable = false)
    private String recommendation;

    @Column(columnDefinition = "TEXT")
    private String reasoning;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public List<String> reasonsList() {
        if (reasonsJson == null || reasonsJson.isBlank()) {
            return List.of();
        }
        return Arrays.stream(reasonsJson.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public static String joinReasons(List<String> reasons) {
        return reasons == null ? "" : String.join("\n", reasons);
    }
}
