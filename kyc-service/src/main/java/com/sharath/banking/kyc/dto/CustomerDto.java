package com.sharath.banking.kyc.dto;

import com.sharath.banking.kyc.entity.Customer;

import java.time.LocalDateTime;

public record CustomerDto(
        Long id,
        String fullName,
        String email,
        String phone,
        String documentType,
        String documentNumber,
        String documentHolderName,
        String address,
        String dateOfBirth,
        String status,
        String riskRating,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CustomerDto from(Customer c) {
        return new CustomerDto(
                c.getId(),
                c.getFullName(),
                c.getEmail(),
                c.getPhone(),
                c.getDocumentType() != null ? c.getDocumentType().name() : null,
                c.getDocumentNumber(),
                c.getDocumentHolderName(),
                c.getAddress(),
                c.getDateOfBirth(),
                c.getStatus() != null ? c.getStatus().name() : null,
                c.getRiskRating() != null ? c.getRiskRating().name() : null,
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
