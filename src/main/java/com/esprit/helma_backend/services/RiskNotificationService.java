package com.esprit.helma_backend.services;

import com.esprit.helma_backend.dto.RiskAlertDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RiskNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public RiskNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendRiskAlert(Long userId, String message, int riskScore, List<String> reasons) {
        System.out.println("=== WEBSOCKET DEBUG === Sending to /topic/risk/" + userId
                + " | Score: " + riskScore
                + " | Level: " + (riskScore >= 70 ? "HIGH" : riskScore >= 40 ? "MEDIUM" : "LOW")
                + " | Reasons: " + reasons
                + " | Message: " + message);

        RiskAlertDto alert = RiskAlertDto.of(userId, message, riskScore, reasons);
        messagingTemplate.convertAndSend("/topic/risk/" + userId, alert);

        System.out.println("=== WEBSOCKET DEBUG === Alert sent successfully to /topic/risk/" + userId);
    }
}
