package com.sharath.banking.loan.repository;
import com.sharath.banking.loan.model.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
    List<LoanApplication> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
