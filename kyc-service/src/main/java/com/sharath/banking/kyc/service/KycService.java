package com.sharath.banking.kyc.service;

import com.sharath.banking.common.dto.AgentResponse;
import com.sharath.banking.kyc.agent.KycAgent;
import com.sharath.banking.kyc.dto.ComplianceReportDto;
import com.sharath.banking.kyc.dto.CreateCustomerRequest;
import com.sharath.banking.kyc.dto.CustomerDto;
import com.sharath.banking.kyc.dto.KycVerificationDto;
import com.sharath.banking.kyc.entity.Customer;
import com.sharath.banking.kyc.entity.KycStatus;
import com.sharath.banking.kyc.repository.CustomerRepository;
import com.sharath.banking.kyc.repository.KycVerificationRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KycService {

    private final CustomerRepository customerRepository;
    private final KycVerificationRepository verificationRepository;
    private final KycAgent kycAgent;

    public KycService(CustomerRepository customerRepository,
                      KycVerificationRepository verificationRepository,
                      KycAgent kycAgent) {
        this.customerRepository = customerRepository;
        this.verificationRepository = verificationRepository;
        this.kycAgent = kycAgent;
    }

    @Transactional
    @CacheEvict(value = "customers", allEntries = true)
    public CustomerDto createCustomer(CreateCustomerRequest request) {
        Customer customer = Customer.builder()
                .fullName(request.fullName())
                .email(request.email())
                .phone(request.phone())
                .documentType(request.documentType())
                .documentNumber(request.documentNumber())
                .documentHolderName(request.documentHolderName() != null
                        ? request.documentHolderName() : request.fullName())
                .address(request.address())
                .dateOfBirth(request.dateOfBirth())
                .status(KycStatus.PENDING)
                .build();
        return CustomerDto.from(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public List<CustomerDto> listCustomers() {
        return customerRepository.findAll().stream().map(CustomerDto::from).toList();
    }

    @Cacheable(value = "customers", key = "#id")
    @Transactional(readOnly = true)
    public CustomerDto getCustomer(Long id) {
        return CustomerDto.from(findCustomer(id));
    }

    @Transactional
    @CacheEvict(value = "customers", key = "#id")
    public AgentResponse<KycVerificationDto> verifyCustomer(Long id) {
        Customer customer = findCustomer(id);
        return kycAgent.verifyCustomer(customer);
    }

    @Transactional(readOnly = true)
    public List<KycVerificationDto> getVerifications(Long customerId) {
        findCustomer(customerId);
        return verificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(KycVerificationDto::from).toList();
    }

    @Transactional(readOnly = true)
    public ComplianceReportDto complianceReport(Long customerId) {
        Customer customer = findCustomer(customerId);
        List<KycVerificationDto> checks = verificationRepository
                .findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(KycVerificationDto::from).toList();
        KycVerificationDto latest = checks.isEmpty() ? null : checks.get(0);
        String reasoning = latest != null ? latest.reasoning()
                : "No KYC verification has been run for this customer yet.";
        return new ComplianceReportDto(
                CustomerDto.from(customer),
                customer.getStatus() != null ? customer.getStatus().name() : null,
                customer.getRiskRating() != null ? customer.getRiskRating().name() : null,
                checks,
                latest,
                reasoning,
                LocalDateTime.now());
    }

    private Customer findCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Customer not found: " + id));
    }
}
