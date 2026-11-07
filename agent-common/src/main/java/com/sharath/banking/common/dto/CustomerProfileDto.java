package com.sharath.banking.common.dto;

public record CustomerProfileDto(Long id, String fullName, String email, String phone, String documentType,
                                 String documentNumber, String address, String dateOfBirth) {
}
