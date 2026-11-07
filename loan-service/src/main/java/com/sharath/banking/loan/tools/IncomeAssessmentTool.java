package com.sharath.banking.loan.tools;

import com.sharath.banking.loan.model.FinancialDocument;
import com.sharath.banking.loan.model.LoanApplication;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

@Component
public class IncomeAssessmentTool {
    public record IncomeResult(BigDecimal declaredMonthlyIncome, BigDecimal verifiedMonthlyIncome,
                               String source, boolean verified) {}

    public IncomeResult assess(LoanApplication application, List<FinancialDocument> documents) {
        BigDecimal declared = application.getMonthlyIncome() != null ? application.getMonthlyIncome() : BigDecimal.ZERO;
        if (documents != null) {
            for (String preferred : List.of("PAYSLIP", "BANK_STATEMENT", "TAX_RETURN")) {
                for (FinancialDocument doc : documents) {
                    if (preferred.equalsIgnoreCase(doc.getDocumentType()) && doc.getVerifiedMonthlyIncome() != null) {
                        return new IncomeResult(declared, doc.getVerifiedMonthlyIncome(), preferred, true);
                    }
                }
            }
        }
        return new IncomeResult(declared, declared, "DECLARED", false);
    }
}
