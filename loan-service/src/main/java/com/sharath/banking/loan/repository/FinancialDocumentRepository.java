package com.sharath.banking.loan.repository;
import com.sharath.banking.loan.model.FinancialDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface FinancialDocumentRepository extends JpaRepository<FinancialDocument, Long> {
    List<FinancialDocument> findByApplicationId(Long applicationId);
}
