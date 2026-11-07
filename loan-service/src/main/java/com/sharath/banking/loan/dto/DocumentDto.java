package com.sharath.banking.loan.dto;
import com.sharath.banking.loan.model.FinancialDocument;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record DocumentDto(Long id, Long applicationId, String documentType, String extractedData,
        BigDecimal verifiedMonthlyIncome, LocalDateTime createdAt) {
    public static DocumentDto from(FinancialDocument d) {
        return new DocumentDto(d.getId(), d.getApplicationId(), d.getDocumentType(), d.getExtractedData(),
                d.getVerifiedMonthlyIncome(), d.getCreatedAt());
    }
}
