package com.sharath.banking.auth.dto;

public record AuthResponse(String token, String username, String role) {
}
