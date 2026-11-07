package com.sharath.banking.kyc.tools;

import com.sharath.banking.kyc.entity.Customer;
import com.sharath.banking.kyc.repository.CustomerRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Tool: persistence access to customer records for the KYC agent. */
@Component
public class CustomerDataTool {

    private final CustomerRepository customerRepository;

    public CustomerDataTool(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    public Optional<Customer> fetchById(Long id) {
        return customerRepository.findById(id);
    }

    public List<Customer> fetchAll() {
        return customerRepository.findAll();
    }

    public boolean existsById(Long id) {
        return customerRepository.existsById(id);
    }
}
