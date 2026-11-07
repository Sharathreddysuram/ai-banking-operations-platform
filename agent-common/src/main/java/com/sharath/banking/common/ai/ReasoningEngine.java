package com.sharath.banking.common.ai;

public interface ReasoningEngine {

    String explain(String agentName, String task, String facts, String ruleBasedExplanation);

    String mode();
}
