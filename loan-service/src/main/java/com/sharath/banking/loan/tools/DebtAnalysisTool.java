package com.sharath.banking.loan.tools;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class DebtAnalysisTool {
    public static final double ASSUMED_ANNUAL_RATE = 0.085;
    public record DebtResult(double dtiRatio, BigDecimal estimatedMonthlyPayment, BigDecimal totalMonthlyDebt) {}

    public BigDecimal estimateMonthlyPayment(BigDecimal principal, int termMonths) {
        if (principal == null || termMonths <= 0) return BigDecimal.ZERO;
        double p = principal.doubleValue();
        double r = ASSUMED_ANNUAL_RATE / 12.0;
        double payment = p * r * Math.pow(1 + r, termMonths) / (Math.pow(1 + r, termMonths) - 1);
        return BigDecimal.valueOf(payment).setScale(2, RoundingMode.HALF_UP);
    }

    public DebtResult analyse(BigDecimal monthlyDebtPayments, BigDecimal monthlyIncome, BigDecimal requestedAmount, int termMonths) {
        BigDecimal debt = monthlyDebtPayments != null ? monthlyDebtPayments : BigDecimal.ZERO;
        BigDecimal payment = estimateMonthlyPayment(requestedAmount, termMonths);
        BigDecimal total = debt.add(payment);
        double dti = (monthlyIncome == null || monthlyIncome.doubleValue() <= 0)
                ? 1.0 : total.doubleValue() / monthlyIncome.doubleValue();
        return new DebtResult(dti, payment, total);
    }
}
