package com.sharath.banking.kyc.controller;

import com.sharath.banking.common.dto.AgentResponse;
import com.sharath.banking.kyc.dto.ComplianceReportDto;
import com.sharath.banking.kyc.dto.CreateCustomerRequest;
import com.sharath.banking.kyc.dto.CustomerDto;
import com.sharath.banking.kyc.dto.KycVerificationDto;
import com.sharath.banking.kyc.service.KycService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/kyc")
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    @PostMapping("/customers")
    public ResponseEntity<CustomerDto> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(kycService.createCustomer(request));
    }

    @GetMapping("/customers")
    public List<CustomerDto> listCustomers() {
        return kycService.listCustomers();
    }

    @GetMapping("/customers/{id}")
    public CustomerDto getCustomer(@PathVariable Long id) {
        return kycService.getCustomer(id);
    }

    @PostMapping("/customers/{id}/verify")
    public AgentResponse<KycVerificationDto> verifyCustomer(@PathVariable Long id) {
        return kycService.verifyCustomer(id);
    }

    @GetMapping("/customers/{id}/verifications")
    public List<KycVerificationDto> getVerifications(@PathVariable Long id) {
        return kycService.getVerifications(id);
    }

    @GetMapping("/customers/{id}/compliance-report")
    public ComplianceReportDto complianceReport(@PathVariable Long id) {
        return kycService.complianceReport(id);
    }
}
