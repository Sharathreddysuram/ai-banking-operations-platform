package com.sharath.banking.support.controller;

import com.sharath.banking.common.dto.AgentResponse;
import com.sharath.banking.support.agent.SupportAgent;
import com.sharath.banking.support.dto.*;
import com.sharath.banking.support.knowledge.KnowledgeCatalog;
import com.sharath.banking.support.model.SupportTicket;
import com.sharath.banking.support.repository.SupportTicketRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {
    private final SupportAgent agent;
    private final SupportTicketRepository ticketRepository;

    @PostMapping("/ask")
    public AgentResponse<TicketDto> ask(@Valid @RequestBody AskRequest req) {
        SupportTicket ticket = agent.ask(req.customerId(), req.question());
        double confidence = ticket.isEscalated() ? 0.45 : 0.88;
        return AgentResponse.of(TicketDto.from(ticket), ticket.getReasoning(), confidence, SupportAgent.AGENT_NAME);
    }

    @GetMapping("/tickets")
    public List<TicketDto> tickets(@RequestParam String customerId) {
        return ticketRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream().map(TicketDto::from).toList();
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<TicketDto> ticket(@PathVariable Long id) {
        return ticketRepository.findById(id).map(t -> ResponseEntity.ok(TicketDto.from(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/tickets/{id}/close")
    public ResponseEntity<TicketDto> close(@PathVariable Long id) {
        return ticketRepository.findById(id).map(t -> {
            t.setStatus("CLOSED");
            return ResponseEntity.ok(TicketDto.from(ticketRepository.save(t)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/knowledge")
    public List<Map<String, String>> knowledge() {
        return KnowledgeCatalog.all().stream()
                .map(d -> Map.of("title", d.title(), "category", d.category())).toList();
    }
}
