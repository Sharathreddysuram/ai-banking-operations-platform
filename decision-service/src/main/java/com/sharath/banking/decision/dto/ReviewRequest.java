package com.sharath.banking.decision.dto;
import jakarta.validation.constraints.NotBlank;
public record ReviewRequest(@NotBlank String decision, String reviewedBy, String notes) {}
