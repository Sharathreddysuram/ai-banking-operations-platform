package com.sharath.banking.common.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ai.mode", havingValue = "azure")
public class AzureReasoningEngine implements ReasoningEngine {

    private final ChatClient chatClient;

    public AzureReasoningEngine(ObjectProvider<ChatModel> chatModelProvider) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        this.chatClient = chatModel != null ? ChatClient.builder(chatModel).build() : null;
    }

    @Override
    public String explain(String agentName, String task, String facts, String ruleBasedExplanation) {
        if (chatClient == null) {
            return "[FALLBACK ENGINE] Agent: " + agentName
                    + " | Task: " + task
                    + " | Facts considered: " + facts
                    + " | Rule-based reasoning: " + ruleBasedExplanation;
        }
        try {
            String content = chatClient.prompt()
                    .system("You are the " + agentName + " in an enterprise banking platform. Explain decisions clearly and concisely for compliance audit.")
                    .user("Task: " + task + "\nFacts: " + facts + "\nRule-based analysis: " + ruleBasedExplanation + "\nWrite a clear explainable reasoning trace (3-6 sentences).")
                    .call()
                    .content();
            return content != null ? content : ruleBasedExplanation;
        } catch (Exception ex) {
            return ruleBasedExplanation;
        }
    }

    @Override
    public String mode() {
        return "azure";
    }
}
