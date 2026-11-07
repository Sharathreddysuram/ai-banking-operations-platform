package com.sharath.banking.kyc.dto;

import com.sharath.banking.kyc.entity.DocumentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCustomerRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        String phone,
        @NotNull DocumentType documentType,
        @NotBlank String documentNumber,
        String documentHolderName,
        String address,
        String dateOfBirth
) {
}
