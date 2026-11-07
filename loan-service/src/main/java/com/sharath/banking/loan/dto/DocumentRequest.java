package com.sharath.banking.loan.dto;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
public record DocumentRequest(@NotBlank String documentType, String extractedData, BigDecimal verifiedMonthlyIncome) {}
