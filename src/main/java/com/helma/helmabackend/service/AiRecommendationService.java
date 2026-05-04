package com.helma.helmabackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helma.helmabackend.entity.SavingsDeposit;
import com.helma.helmabackend.entity.SavingsGoal;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class AiRecommendationService {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://openrouter.ai/api/v1/chat/completions")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("Authorization", "Bearer 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")
            .build();

    public String generateAdvice(SavingsGoal goal, List<SavingsDeposit> deposits) {

        try {

            double totalDeposited = deposits.stream()
                    .mapToDouble(SavingsDeposit::getAmount)
                    .sum();

            int depositCount = deposits.size();

            String prompt =
                    "You are a financial advisor inside a banking mobile app.\n" +
                            "User goal: " + goal.getTitle() + "\n" +
                            "Target amount: " + goal.getTargetAmount() + "\n" +
                            "Current saved: " + goal.getCurrentAmount() + "\n" +
                            "Deadline: " + goal.getDeadline() + "\n" +
                            "Number of deposits: " + depositCount + "\n" +
                            "Total deposited: " + totalDeposited + "\n" +
                            "Give short personalized saving advice.";

            String body = """
            {
              "model": "mistralai/mistral-7b-instruct",
              "messages": [
                {"role": "user", "content": "%s"}
              ]
            }
            """.formatted(prompt.replace("\"","\\\""));

            String response = webClient.post()
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);

            return root
                    .get("choices")
                    .get(0)
                    .get("message")
                    .get("content")
                    .asText();

        } catch (Exception e) {
            return "AI recommendation unavailable.";
        }
    }
}
