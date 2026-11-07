package com.sharath.banking.support.tools;

import com.sharath.banking.support.model.CustomerAccount;
import com.sharath.banking.support.repository.CustomerAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CustomerDataTool {
    private final CustomerAccountRepository repository;

    public List<CustomerAccount> getAccounts(String customerId) {
        return repository.findByCustomerId(customerId);
    }
    public BigDecimal totalBalance(String customerId) {
        return getAccounts(customerId).stream().map(CustomerAccount::getBalance)
                .filter(b -> b != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    public String accountSummary(String customerId) {
        List<CustomerAccount> accounts = getAccounts(customerId);
        if (accounts.isEmpty()) return "No accounts found for customer " + customerId + ".";
        return accounts.stream()
                .map(a -> a.getAccountType() + " account " + a.getAccountNumber() + ": balance $" + a.getBalance() + " (" + a.getStatus() + ")")
                .collect(Collectors.joining("; "));
    }
}
