package com.sharath.banking.loan.dto;
import com.sharath.banking.loan.model.LoanApplication;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record ApplicationDto(Long id, String customerId, BigDecimal requestedAmount, int termMonths, String purpose,
        BigDecimal monthlyIncome, BigDecimal monthlyDebtPayments, BigDecimal annualIncome, int creditScore,
        String employmentStatus, String status, LocalDateTime createdAt) {
    public static ApplicationDto from(LoanApplication a) {
        return new ApplicationDto(a.getId(), a.getCustomerId(), a.getRequestedAmount(), a.getTermMonths(), a.getPurpose(),
                a.getMonthlyIncome(), a.getMonthlyDebtPayments(), a.getAnnualIncome(), a.getCreditScore(),
                a.getEmploymentStatus(), a.getStatus() != null ? a.getStatus().name() : null, a.getCreatedAt());
    }
}
