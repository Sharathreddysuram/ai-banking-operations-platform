package com.sharath.banking.loan.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record ApplicationRequest(
        @NotBlank String customerId,
        @NotNull @DecimalMin("100") BigDecimal requestedAmount,
        @Min(6) @Max(84) int termMonths,
        String purpose,
        @NotNull @DecimalMin("0") BigDecimal monthlyIncome,
        @DecimalMin("0") BigDecimal monthlyDebtPayments,
        BigDecimal annualIncome,
        @Min(300) @Max(850) int creditScore,
        String employmentStatus) {}
