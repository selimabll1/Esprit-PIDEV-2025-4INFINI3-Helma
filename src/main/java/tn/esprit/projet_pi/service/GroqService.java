package tn.esprit.projet_pi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class GroqService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    @Value("${groq.model}")
    private String groqModel;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String ask(String prompt) {
        try {
            String apiKey = groqApiKey == null ? "" : groqApiKey.trim();
            if (apiKey.isBlank() || !apiKey.startsWith("gsk_")) {
                return "Analyse IA indisponible: cle Groq absente ou invalide. Configure GROQ_API_KEY puis redemarre loan-service.";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ObjectNode bodyNode = objectMapper.createObjectNode();
            bodyNode.put("model", groqModel);
            bodyNode.put("temperature", 0.25);
            bodyNode.put("max_tokens", 700);

            ArrayNode messages = objectMapper.createArrayNode();
            ObjectNode message = objectMapper.createObjectNode();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            bodyNode.set("messages", messages);

            ResponseEntity<String> response = restTemplate.exchange(
                    groqApiUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(bodyNode), headers),
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").path(0).path("message").path("content").asText().trim();
        } catch (Exception e) {
            log.error("Erreur appel Groq: {}", e.getMessage(), e);
            if (e.getMessage() != null && e.getMessage().contains("401 Unauthorized")) {
                return "Analyse IA indisponible: cle Groq refusee (401 Unauthorized). Regenerer GROQ_API_KEY puis redemarrer loan-service.";
            }
            return "Analyse IA indisponible: " + e.getMessage();
        }
    }
}
