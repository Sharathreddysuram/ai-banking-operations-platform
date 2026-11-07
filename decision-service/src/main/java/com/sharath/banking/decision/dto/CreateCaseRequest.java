package com.sharath.banking.decision.dto;
import jakarta.validation.constraints.NotBlank;
public record CreateCaseRequest(@NotBlank String customerId, String caseType) {}
