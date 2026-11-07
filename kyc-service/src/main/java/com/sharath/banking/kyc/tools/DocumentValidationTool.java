package com.sharath.banking.kyc.tools;

import com.sharath.banking.kyc.entity.DocumentType;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Tool: validates document number formats and applies an expiry heuristic.
 * Demo expiry heuristic: a document number containing the marker "EXP"
 * or ending in "0000" is treated as an expired legacy document; any
 * well-formed document is otherwise treated as not expired.
 */
@Component
public class DocumentValidationTool {

    private static final Pattern PASSPORT = Pattern.compile("^[A-Z][0-9]{7}$");
    private static final Pattern DRIVERS_LICENSE = Pattern.compile("^[A-Z0-9]{8,12}$");
    private static final Pattern NATIONAL_ID = Pattern.compile("^[0-9]{9}$");

    public record DocumentValidationResult(
            boolean formatValid,
            boolean expired,
            String documentType,
            String documentNumber,
            String message) {
    }

    public DocumentValidationResult validate(DocumentType documentType, String documentNumber) {
        if (documentType == null || documentNumber == null || documentNumber.isBlank()) {
            return new DocumentValidationResult(false, false,
                    documentType != null ? documentType.name() : null, documentNumber,
                    "Document type or number is missing");
        }
        String normalized = documentNumber.trim().toUpperCase();
        boolean valid = switch (documentType) {
            case PASSPORT -> PASSPORT.matcher(normalized).matches();
            case DRIVERS_LICENSE -> DRIVERS_LICENSE.matcher(normalized).matches();
            case NATIONAL_ID -> NATIONAL_ID.matcher(normalized).matches();
        };
        boolean expired = checkExpiryHeuristic(normalized);
        String message;
        if (!valid) {
            message = "Document number '" + documentNumber + "' does not match the expected "
                    + documentType.name() + " format";
        } else if (expired) {
            message = "Document number '" + documentNumber + "' matches the format but is flagged as expired by the expiry heuristic";
        } else {
            message = "Document number '" + documentNumber + "' matches the expected "
                    + documentType.name() + " format and is not expired";
        }
        return new DocumentValidationResult(valid, expired, documentType.name(), documentNumber, message);
    }

    public boolean checkExpiryHeuristic(String normalizedDocumentNumber) {
        if (normalizedDocumentNumber == null) {
            return false;
        }
        return normalizedDocumentNumber.contains("EXP") || normalizedDocumentNumber.endsWith("0000");
    }
}
