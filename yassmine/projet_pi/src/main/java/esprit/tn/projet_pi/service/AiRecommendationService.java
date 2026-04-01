package esprit.tn.projet_pi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import esprit.tn.projet_pi.entity.SavingsDeposit;
import esprit.tn.projet_pi.entity.SavingsGoal;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class AiRecommendationService {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://openrouter.ai/api/v1")
            .defaultHeader("Authorization", "Bearer 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")
            .defaultHeader("HTTP-Referer", "http://localhost:8080")
            .defaultHeader("X-Title", "ProjetPi")
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
              "model": "meta-llama/llama-3.2-3b-instruct:free",
              "messages": [
                {"role": "user", "content": "%s"}
              ]
            }
            """.formatted(prompt.replace("\"", "\\\""));

            String response = webClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            return root.get("choices").get(0).get("message").get("content").asText();

        } catch (Exception e) {
            return "AI recommendation unavailable. Error: " + e.getMessage();
        }
    }
}