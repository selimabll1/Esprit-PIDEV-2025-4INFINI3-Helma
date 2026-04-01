package com.esprit.helma_backend.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GroqApiClient {

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";
    private static final String FALLBACK_MESSAGE =
            "Je ne suis pas disponible pour le moment. Vérifiez vos données et réessayez.";

    @Value("${groq.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GroqApiClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void debugKey() {
        String key = normalizedKey();
        System.out.println("=== GROQ DEBUG ===");
        System.out.println("Groq key loaded? " + (key != null && !key.isBlank()));
        if (key != null && !key.isBlank()) {
            System.out.println("Groq key prefix: " + key.substring(0, Math.min(12, key.length())));
            System.out.println("Groq key length: " + key.length());
            System.out.println("Groq key startsWith gsk_: " + key.startsWith("gsk_"));
        }
        System.out.println("==================");
    }

    public String getKeyDebugInfo() {
        String raw = apiKey;
        String key = normalizedKey();

        return "rawLoaded=" + (raw != null) +
                ", rawBlank=" + (raw == null || raw.isBlank()) +
                ", normalizedBlank=" + (key == null || key.isBlank()) +
                ", prefix=" + ((key == null || key.isBlank()) ? "NONE" : key.substring(0, Math.min(12, key.length()))) +
                ", length=" + ((key == null) ? 0 : key.length()) +
                ", startsWith_gsk=" + (key != null && key.startsWith("gsk_"));
    }

    public String chat(List<Map<String, String>> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new RuntimeException("Groq error: messages vides");
        }

        String key = normalizedKey();
        if (key == null || key.isBlank()) {
            throw new RuntimeException("Groq error: groq.api.key manquante ou vide");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", MODEL);
            body.put("messages", messages);
            body.put("max_tokens", 1024);
            body.put("temperature", 0.7);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(key);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    GROQ_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Groq error: HTTP " + response.getStatusCode().value());
            }

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isBlank()) {
                throw new RuntimeException("Groq error: réponse vide");
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");

            if (contentNode.isMissingNode() || contentNode.isNull()) {
                throw new RuntimeException("Groq error: champ choices[0].message.content introuvable. Réponse brute = " + responseBody);
            }

            String content = contentNode.asText();
            if (content == null || content.isBlank()) {
                throw new RuntimeException("Groq error: contenu vide dans la réponse");
            }

            return content.trim();

        } catch (HttpStatusCodeException e) {
            String responseBody = e.getResponseBodyAsString();
            throw new RuntimeException(
                    "Groq HTTP error: " + e.getStatusCode().value() + " - " +
                            (responseBody == null || responseBody.isBlank() ? e.getMessage() : responseBody),
                    e
            );
        } catch (RestClientException e) {
            throw new RuntimeException("Groq transport error: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Groq parse/internal error: " + e.getMessage(), e);
        }
    }

    public String safeChat(List<Map<String, String>> messages) {
        try {
            return chat(messages);
        } catch (Exception e) {
            e.printStackTrace();
            return FALLBACK_MESSAGE;
        }
    }

    private String normalizedKey() {
        if (apiKey == null) return null;
        String key = apiKey.trim();

        if (key.startsWith("\"") && key.endsWith("\"") && key.length() >= 2) {
            key = key.substring(1, key.length() - 1).trim();
        }

        if (key.startsWith("'") && key.endsWith("'") && key.length() >= 2) {
            key = key.substring(1, key.length() - 1).trim();
        }

        return key;
    }
}