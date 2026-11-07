package com.sharath.banking.support.dto;
import jakarta.validation.constraints.NotBlank;
public record AskRequest(@NotBlank String customerId, @NotBlank String question) {}
