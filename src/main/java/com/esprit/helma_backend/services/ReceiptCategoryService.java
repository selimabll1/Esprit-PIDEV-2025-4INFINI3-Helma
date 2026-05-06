package com.esprit.helma_backend.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends a receipt image (base64) to Groq's vision model
 * and returns a single category string.
 *
 * Groq vision model: meta-llama/llama-4-scout-17b-16e-instruct
 */
// Superseded by ReceiptScanService — kept for reference only, not a Spring bean
public class ReceiptCategoryService {

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String VISION_MODEL = "meta-llama/llama-4-scout-17b-16e-instruct";

    private static final String PROMPT = """
            You are a receipt classifier. Look at this receipt image carefully.
            Based on the store name, logo, items listed, or any visible text,
            classify this expense into EXACTLY ONE of these categories:
            groceries, food, transport, health, shopping, utilities, entertainment, other

            Rules:
            - Supermarkets (Carrefour, Monoprix, Geant, etc.) → groceries
            - Restaurants, cafes, fast food, delivery apps → food
            - Train, bus, taxi, fuel, parking, Uber → transport
            - Pharmacy, doctor, clinic, lab → health
            - Clothing, electronics, general retail → shopping
            - Electricity, water, internet, phone bills → utilities
            - Cinema, streaming, games, sports → entertainment
            - Anything else → other

            Reply with ONLY the single category word, nothing else. No punctuation, no explanation.
            """;

    @Value("${groq.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ReceiptCategoryService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String suggestCategory(String base64Image) {
        try {
            String key = normalizedKey();
            if (key == null || key.isBlank()) {
                return "other";
            }

            // Build vision message with image_url content block
            Map<String, Object> textBlock = new HashMap<>();
            textBlock.put("type", "text");
            textBlock.put("text", PROMPT);

            Map<String, Object> imageUrl = new HashMap<>();
            imageUrl.put("url", base64Image); // data:image/jpeg;base64,...

            Map<String, Object> imageBlock = new HashMap<>();
            imageBlock.put("type", "image_url");
            imageBlock.put("image_url", imageUrl);

            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", List.of(textBlock, imageBlock));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", VISION_MODEL);
            requestBody.put("messages", List.of(userMessage));
            requestBody.put("max_tokens", 20);
            requestBody.put("temperature", 0.1); // low temp = deterministic

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(key);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    GROQ_URL, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return "other";
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            String raw = root.path("choices").path(0)
                    .path("message").path("content").asText("other");

            return sanitize(raw.trim().toLowerCase());

        } catch (Exception e) {
            System.err.println("ReceiptCategoryService error: " + e.getMessage());
            return "other";
        }
    }

    /** Ensure we only return valid category values */
    private String sanitize(String raw) {
        List<String> valid = List.of(
                "groceries", "food", "transport", "health",
                "shopping", "utilities", "entertainment", "other");
        // Strip any extra punctuation the model might add
        String cleaned = raw.replaceAll("[^a-z]", "");
        return valid.contains(cleaned) ? cleaned : "other";
    }

    private String normalizedKey() {
        if (apiKey == null) return null;
        String key = apiKey.trim();
        if (key.startsWith("\"") && key.endsWith("\"") && key.length() >= 2)
            key = key.substring(1, key.length() - 1).trim();
        if (key.startsWith("'") && key.endsWith("'") && key.length() >= 2)
            key = key.substring(1, key.length() - 1).trim();
        return key;
    }
}
