package com.sharath.banking.loan.repository;
import com.sharath.banking.loan.model.UnderwritingAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UnderwritingAssessmentRepository extends JpaRepository<UnderwritingAssessment, Long> {
    Optional<UnderwritingAssessment> findTopByApplicationIdOrderByCreatedAtDesc(Long applicationId);
}
