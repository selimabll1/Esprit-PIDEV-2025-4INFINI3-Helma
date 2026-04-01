package esprit.tn.projet_pi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import esprit.tn.projet_pi.entity.SavingsDeposit;
import esprit.tn.projet_pi.entity.SavingsGoal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class AiRecommendationService {
    //  WebClient = client HTTP (fourni par WebFlux) utilisé pour appeler une API externe (OpenRouter)
    private final WebClient webClient = WebClient.builder()
            //  baseUrl = endpoint de l'API OpenRouter pour "chat completions"

            .baseUrl("https://openrouter.ai/api/v1/chat/completions")
            //   le contenu envoyé sera du JSON.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("Authorization", "Bearer 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")
            .build();

    public String generateAdvice(SavingsGoal goal, List<SavingsDeposit> deposits) {

        try {
            // totalDeposited = somme des montants déposés
            double totalDeposited = deposits.stream()
                    .mapToDouble(SavingsDeposit::getAmount)
                    .sum();
            // depositCount = nombre total de dépôts
            int depositCount = deposits.size();
            //  prompt = texte envoyé à l'IA (contexte + instruction)
            // 1) On définit le rôle de l'IA : "financial advisor"
            // 2) On injecte des infos du user (goal + dépôts)
            // 3) On demande un résultat précis : "short personalized saving advice"
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
            //  Parsing JSON : OpenRouter renvoie un JSON complexe
            // ObjectMapper (Jackson) permet de lire la réponse et extraire le texte utile
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            //  Extraction du résultat :
            return root
                    .get("choices")
                    .get(0)
                    .get("message")
                    .get("content")
                    .asText();

        } catch (Exception e) {
            //  Fallback : si l'API tombe en panne / timeout / JSON invalide
            return "AI recommendation unavailable.";
        }
    }
}
