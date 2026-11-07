package com.sharath.banking.fraud.repository;

import com.sharath.banking.fraud.entity.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {
    List<BankTransaction> findByCustomerIdOrderByTimestampDesc(String customerId);
    List<BankTransaction> findByCustomerIdAndTimestampAfterOrderByTimestampDesc(String customerId, LocalDateTime after);
    long countByCustomerIdAndTimestampAfter(String customerId, LocalDateTime after);
    long countByCustomerId(String customerId);
}
