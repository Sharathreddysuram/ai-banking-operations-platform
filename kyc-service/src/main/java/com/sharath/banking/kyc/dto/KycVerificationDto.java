package com.sharath.banking.kyc.dto;

import com.sharath.banking.kyc.entity.KycVerification;

import java.time.LocalDateTime;

public record KycVerificationDto(
        Long id,
        Long customerId,
        boolean documentFormatValid,
        double nameMatchScore,
        boolean documentExpired,
        boolean sanctionsHit,
        double verificationScore,
        String checksJson,
        String decision,
        String reasoning,
        LocalDateTime createdAt
) {
    public static KycVerificationDto from(KycVerification v) {
        return new KycVerificationDto(
                v.getId(),
                v.getCustomerId(),
                v.isDocumentFormatValid(),
                v.getNameMatchScore(),
                v.isDocumentExpired(),
                v.isSanctionsHit(),
                v.getVerificationScore(),
                v.getChecksJson(),
                v.getDecision(),
                v.getReasoning(),
                v.getCreatedAt());
    }
}
