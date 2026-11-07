package com.sharath.banking.loan.tools;

import org.springframework.stereotype.Component;

@Component
public class CreditRiskTool {
    public String band(int creditScore) {
        if (creditScore >= 750) return "EXCELLENT";
        if (creditScore >= 700) return "GOOD";
        if (creditScore >= 670) return "FAIR";
        if (creditScore >= 580) return "POOR";
        return "VERY_POOR";
    }
    /** Maps 850 -> 0 risk, 300 -> 100 risk, linearly. */
    public double scoreToRisk(int creditScore) {
        double risk = (850.0 - creditScore) / 550.0 * 100.0;
        return Math.max(0, Math.min(100, risk));
    }
}
