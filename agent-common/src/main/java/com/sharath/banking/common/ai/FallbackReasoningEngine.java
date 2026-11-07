package com.sharath.banking.common.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ai.mode", havingValue = "fallback", matchIfMissing = true)
public class FallbackReasoningEngine implements ReasoningEngine {

    @Override
    public String explain(String agentName, String task, String facts, String ruleBasedExplanation) {
        return "[FALLBACK ENGINE] Agent: " + agentName
                + " | Task: " + task
                + " | Facts considered: " + facts
                + " | Rule-based reasoning: " + ruleBasedExplanation;
    }

    @Override
    public String mode() {
        return "fallback";
    }
}
