package com.sharath.banking.support.dto;
import com.sharath.banking.support.model.SupportTicket;
import java.time.LocalDateTime;
public record TicketDto(Long id, String customerId, String subject, String question, String answer,
        String category, String status, boolean escalated, String reasoning, LocalDateTime createdAt) {
    public static TicketDto from(SupportTicket t) {
        return new TicketDto(t.getId(), t.getCustomerId(), t.getSubject(), t.getQuestion(), t.getAnswer(),
                t.getCategory(), t.getStatus(), t.isEscalated(), t.getReasoning(), t.getCreatedAt());
    }
}
