package com.sharath.banking.loan.dto;
import com.sharath.banking.loan.model.UnderwritingAssessment;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
public record AssessmentDto(Long id, Long applicationId, double dtiRatio, double riskScore, String riskBand,
        String recommendation, BigDecimal recommendedAmount, String reasoning, List<String> factors, LocalDateTime createdAt) {
    public static AssessmentDto from(UnderwritingAssessment a) {
        List<String> factors = a.getFactorsJson() == null || a.getFactorsJson().isBlank()
                ? List.of() : Arrays.asList(a.getFactorsJson().split("\n"));
        return new AssessmentDto(a.getId(), a.getApplicationId(), a.getDtiRatio(), a.getRiskScore(), a.getRiskBand(),
                a.getRecommendation(), a.getRecommendedAmount(), a.getReasoning(), factors, a.getCreatedAt());
    }
}
