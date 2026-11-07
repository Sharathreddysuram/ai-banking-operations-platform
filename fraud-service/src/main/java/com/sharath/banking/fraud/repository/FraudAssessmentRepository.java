package com.sharath.banking.fraud.repository;

import com.sharath.banking.fraud.entity.FraudAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FraudAssessmentRepository extends JpaRepository<FraudAssessment, Long> {
    List<FraudAssessment> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    List<FraudAssessment> findBySeverityInOrderByCreatedAtDesc(Collection<String> severities);
    Optional<FraudAssessment> findFirstByTransactionIdOrderByCreatedAtDesc(Long transactionId);
    List<FraudAssessment> findByTransactionIdOrderByCreatedAtDesc(Long transactionId);
}
