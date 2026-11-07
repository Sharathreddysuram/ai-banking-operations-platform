package com.sharath.banking.auth.dto;

public record UserInfoResponse(Long id, String username, String email, String fullName, String role, boolean enabled) {
}
