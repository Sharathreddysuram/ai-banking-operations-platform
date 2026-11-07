package com.sharath.banking.fraud.tools;

import com.sharath.banking.fraud.entity.BankTransaction;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Tool: detects rapid location changes and foreign-location activity. */
@Component
public class GeoVelocityTool {

    public record GeoVelocityResult(
            boolean rapidLocationChange,
            String previousLocation,
            String currentLocation,
            long minutesBetween,
            String message) {
    }

    /**
     * Flags a rapid location change when the most recent prior transaction
     * was in a different location within 60 minutes of the current one.
     */
    public GeoVelocityResult check(BankTransaction current, List<BankTransaction> historyExcludingCurrent) {
        if (current == null || current.getTimestamp() == null || historyExcludingCurrent == null
                || historyExcludingCurrent.isEmpty()) {
            return new GeoVelocityResult(false, null,
                    current != null ? current.getLocation() : null, -1, "No prior history to compare locations");
        }
        BankTransaction previous = null;
        for (BankTransaction candidate : historyExcludingCurrent) {
            if (candidate.getTimestamp() != null && !candidate.getTimestamp().isAfter(current.getTimestamp())) {
                if (previous == null || candidate.getTimestamp().isAfter(previous.getTimestamp())) {
                    previous = candidate;
                }
            }
        }
        if (previous == null || previous.getLocation() == null || current.getLocation() == null) {
            return new GeoVelocityResult(false, null, current.getLocation(), -1, "No comparable prior location");
        }
        long minutes = Math.abs(Duration.between(previous.getTimestamp(), current.getTimestamp()).toMinutes());
        boolean different = !previous.getLocation().trim().equalsIgnoreCase(current.getLocation().trim());
        boolean rapid = different && minutes <= 60;
        String message = rapid
                ? "Location changed from '" + previous.getLocation() + "' to '" + current.getLocation()
                        + "' within " + minutes + " minutes"
                : "No rapid location change (previous location '" + previous.getLocation()
                        + "', " + minutes + " minutes before)";
        return new GeoVelocityResult(rapid, previous.getLocation(), current.getLocation(), minutes, message);
    }

    /** Country suffix after the last comma, e.g. "Austin, US" -> "US". */
    public String countryOf(String location) {
        if (location == null) {
            return null;
        }
        int idx = location.lastIndexOf(',');
        String country = (idx >= 0 ? location.substring(idx + 1) : location).trim();
        return country.isEmpty() ? null : country.toUpperCase(Locale.ROOT);
    }

    /** Most frequent country in the customer's history = home country. */
    public String homeCountry(List<BankTransaction> history) {
        if (history == null || history.isEmpty()) {
            return null;
        }
        return history.stream()
                .map(t -> countryOf(t.getLocation()))
                .filter(c -> c != null)
                .collect(Collectors.groupingBy(c -> c, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public boolean isForeignLocation(BankTransaction current, List<BankTransaction> history) {
        String home = homeCountry(history);
        String currentCountry = countryOf(current != null ? current.getLocation() : null);
        return home != null && currentCountry != null && !home.equals(currentCountry);
    }
}
