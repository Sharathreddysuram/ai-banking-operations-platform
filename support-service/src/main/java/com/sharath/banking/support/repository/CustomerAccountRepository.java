package com.sharath.banking.support.repository;
import com.sharath.banking.support.model.CustomerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, Long> {
    List<CustomerAccount> findByCustomerId(String customerId);
}
