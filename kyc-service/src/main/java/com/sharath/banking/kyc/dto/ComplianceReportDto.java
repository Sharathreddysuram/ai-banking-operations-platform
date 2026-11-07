package com.sharath.banking.kyc.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ComplianceReportDto(
        CustomerDto customer,
        String status,
        String riskRating,
        List<KycVerificationDto> checks,
        KycVerificationDto latestVerification,
        String reasoning,
        LocalDateTime generatedAt
) {
}
