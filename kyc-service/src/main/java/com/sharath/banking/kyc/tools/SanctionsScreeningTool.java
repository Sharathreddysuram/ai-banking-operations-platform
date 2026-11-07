package com.sharath.banking.kyc.tools;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Tool: screens a customer name against an in-code demo sanctions watchlist.
 * A hit is returned when the screened name contains any watchlist entry
 * or any significant token (word longer than 3 characters) of a watchlist entry.
 */
@Component
public class SanctionsScreeningTool {

    private static final List<String> WATCHLIST = List.of(
            "John Doe Fraud",
            "Ivan Petrov",
            "Test Sanction",
            "Sanctioned Person",
            "Money Launderer",
            "Viktor Kozlov",
            "Carlos Mendoza Fraud"
    );

    public record SanctionsScreeningResult(
            boolean hit,
            String matchedEntry,
            String screenedName,
            String message) {
    }

    public List<String> watchlist() {
        return WATCHLIST;
    }

    public SanctionsScreeningResult screen(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new SanctionsScreeningResult(false, null, fullName, "No name supplied for screening");
        }
        String normalizedName = fullName.toLowerCase(Locale.ROOT);
        for (String entry : WATCHLIST) {
            String normalizedEntry = entry.toLowerCase(Locale.ROOT);
            if (normalizedName.contains(normalizedEntry)) {
                return new SanctionsScreeningResult(true, entry, fullName,
                        "Name exactly matches watchlist entry '" + entry + "'");
            }
            for (String token : normalizedEntry.split("\s+")) {
                if (token.length() > 3 && normalizedName.contains(token)) {
                    return new SanctionsScreeningResult(true, entry, fullName,
                            "Name contains watchlist token '" + token + "' from entry '" + entry + "'");
                }
            }
        }
        return new SanctionsScreeningResult(false, null, fullName,
                "No match against the sanctions watchlist");
    }
}
