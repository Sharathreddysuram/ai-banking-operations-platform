package com.sharath.banking.common.dto;

public record AgentResponse<T>(T data, String reasoning, double confidence, String agentName) {

    public static <T> AgentResponse<T> of(T data, String reasoning, double confidence, String agentName) {
        return new AgentResponse<>(data, reasoning, confidence, agentName);
    }
}
