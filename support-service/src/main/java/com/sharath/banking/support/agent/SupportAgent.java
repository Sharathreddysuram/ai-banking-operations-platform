package com.sharath.banking.support.agent;

import com.sharath.banking.common.ai.ReasoningEngine;
import com.sharath.banking.common.event.AgentEvent;
import com.sharath.banking.common.event.Topics;
import com.sharath.banking.common.service.AuditService;
import com.sharath.banking.support.model.CustomerAccount;
import com.sharath.banking.support.model.SupportTicket;
import com.sharath.banking.support.repository.SupportTicketRepository;
import com.sharath.banking.support.tools.CustomerDataTool;
import com.sharath.banking.support.tools.KnowledgeBaseTool;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportAgent {
    public static final String AGENT_NAME = "Customer Support Agent";
    private static final List<String> ESCALATION_KEYWORDS = List.of(
            "dispute", "fraud", "legal", "complaint", "close account", "close my account",
            "loan rejection", "appeal", "lawyer", "lawsuit", "unauthorized", "chargeback dispute");
    private final SupportTicketRepository ticketRepository;
    private final KnowledgeBaseTool knowledgeBaseTool;
    private final CustomerDataTool customerDataTool;
    private final ReasoningEngine reasoningEngine;
    private final AuditService auditService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public SupportTicket ask(String customerId, String question) {
        String lower = question == null ? "" : question.toLowerCase();
        List<Document> docs = knowledgeBaseTool.search(question);
        int topOverlap = knowledgeBaseTool.topKeywordScore(question);
        boolean keywordEscalation = ESCALATION_KEYWORDS.stream().anyMatch(lower::contains);
        boolean lowConfidence = topOverlap == 0;
        boolean accountSpecific = lower.contains("my balance") || lower.contains("my account") || lower.contains("account balance");
        boolean escalated = keywordEscalation || (lowConfidence && !accountSpecific);

        String category = "GENERAL";
        String docAnswer = null;
        if (!docs.isEmpty()) {
            Document top = docs.get(0);
            Object cat = top.getMetadata().get("category");
            if (cat != null) category = cat.toString();
            docAnswer = top.getText();
        }

        String answer;
        if (accountSpecific) {
            List<CustomerAccount> accounts = customerDataTool.getAccounts(customerId);
            if (!accounts.isEmpty()) {
                category = "ACCOUNTS";
                answer = "Here is your account summary, " + customerId + ": " + customerDataTool.accountSummary(customerId)
                        + ". Your total balance is $" + customerDataTool.totalBalance(customerId) + ".";
                escalated = keywordEscalation;
            } else {
                answer = "I could not find any accounts for customer " + customerId + ". Please verify your customer ID or contact support.";
            }
        } else if (escalated && keywordEscalation) {
            answer = "Your request involves a sensitive matter that requires a specialist. I have escalated it to our human support team, who will contact you shortly. "
                    + (docAnswer != null ? "For reference: " + docAnswer : "");
            category = lower.contains("fraud") || lower.contains("dispute") || lower.contains("unauthorized") ? "FRAUD" : category;
        } else if (docAnswer != null) {
            answer = docAnswer;
        } else {
            answer = "I could not find a confident answer in our knowledge base, so I have escalated your question to our human support team.";
        }

        String status = escalated ? "ESCALATED" : "ANSWERED";
        String subject = question.length() > 80 ? question.substring(0, 80) : question;
        String facts = "customerId=" + customerId + ", topKeywordOverlap=" + topOverlap + ", docsRetrieved=" + docs.size()
                + ", accountSpecific=" + accountSpecific + ", escalated=" + escalated + ", category=" + category;
        String baseReasoning = "Retrieved " + docs.size() + " knowledge documents (top keyword overlap " + topOverlap + "). "
                + "Escalation keywords present: " + keywordEscalation + ". Account-specific: " + accountSpecific + ". Outcome: " + status + ".";
        String reasoning = reasoningEngine.explain(AGENT_NAME, "ANSWER_QUERY", facts, baseReasoning);

        SupportTicket ticket = ticketRepository.save(SupportTicket.builder()
                .customerId(customerId).subject(subject).question(question).answer(answer)
                .category(category).status(status).escalated(escalated).reasoning(reasoning).build());
        auditService.record(AGENT_NAME, "ANSWER_QUERY", customerId,
                "question: " + subject, reasoning, status + " category=" + category);
        if (escalated) {
            try {
                kafkaTemplate.send(Topics.SUPPORT_ESCALATION, customerId,
                        new AgentEvent(Topics.SUPPORT_ESCALATION, AGENT_NAME, customerId,
                                "ticketId=" + ticket.getId() + " category=" + category + " question=" + subject, Instant.now()));
            } catch (Exception ignored) {
                // Best-effort publish; ticket and audit are already persisted.
            }
        }
        return ticket;
    }
}
