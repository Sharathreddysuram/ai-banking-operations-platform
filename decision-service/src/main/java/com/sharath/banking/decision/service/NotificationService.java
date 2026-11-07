package com.sharath.banking.decision.service;
import com.sharath.banking.decision.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final SimpMessagingTemplate messagingTemplate;
    public void send(NotificationDto notification) {
        try { messagingTemplate.convertAndSend("/topic/notifications", notification); } catch (Exception ignored) {}
    }
    public void notify(String type, String title, String message) {
        send(NotificationDto.of(type, title, message));
    }
}
