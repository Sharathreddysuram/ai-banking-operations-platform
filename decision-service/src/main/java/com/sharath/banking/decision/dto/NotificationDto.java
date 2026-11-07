package com.sharath.banking.decision.dto;
import java.time.Instant;
public record NotificationDto(String type, String title, String message, Instant timestamp) {
    public static NotificationDto of(String type, String title, String message) {
        return new NotificationDto(type, title, message, Instant.now());
    }
}
