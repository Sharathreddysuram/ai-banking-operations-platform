package com.sharath.banking.decision.repository;
import com.sharath.banking.decision.model.DecisionCase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DecisionCaseRepository extends JpaRepository<DecisionCase, Long> {
    List<DecisionCase> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    List<DecisionCase> findAllByOrderByCreatedAtDesc();
}
