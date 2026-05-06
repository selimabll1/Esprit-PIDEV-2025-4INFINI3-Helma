package com.esprit.helma_backend.services;

import com.esprit.helma_backend.dto.ReceiptScanDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReceiptScanService {

    private static final String GROQ_URL    = "https://api.groq.com/openai/v1/chat/completions";
    private static final String VISION_MODEL = "meta-llama/llama-4-scout-17b-16e-instruct";

    private static final String PROMPT = """
            You are a receipt data extractor. Examine this receipt image carefully.
            Respond with ONLY a JSON object — no text before or after, no markdown, no code fences.

            Use exactly this format (replace the example values with real ones from the receipt):
            {"category":"groceries","type":"EXPENSE","amount":47.50,"date":"2026-01-15","description":"pain, lait, fromage, eau"}

            Field rules:

            category — pick ONE: groceries, food, transport, health, shopping, utilities, entertainment, salary, other
              groceries=supermarkets/food stores  food=restaurants/cafes/fast-food
              transport=taxi/fuel/parking/bus/train  health=pharmacy/clinic/doctor
              shopping=clothing/electronics/retail  utilities=electricity/water/internet/phone-bill
              entertainment=cinema/games/streaming  salary=payslip  other=everything else

            type — write EXPENSE for any purchase. Write INCOME only for salary slips, refunds, or invoices you received.

            amount — THE SINGLE FINAL TOTAL the customer paid.
              Step 1: find a line containing one of these exact words (case-insensitive):
                TOTAL, GRAND TOTAL, AMOUNT DUE, NET AMOUNT, TOTAL TTC, TOTAL A PAYER,
                MONTANT TOTAL, TO PAY, SOLDE, BALANCE DUE, TOTAL PAYABLE, NET A PAYER
              Step 2: read the number on that same line or the line immediately below it.
              Step 3: write that number as digits only, no currency symbol, no quotes. Example: 23.40
              If the receipt has both a SUBTOTAL and a TOTAL, always use the TOTAL (it is larger and at the bottom).
              If you cannot find any total label at all, write null (no quotes).

            date — the purchase date printed on the receipt. Format: YYYY-MM-DD. Write null if absent.

            description — list the items purchased, comma-separated, as they appear on the receipt.
              Example: "pain complet, lait demi-ecreme, eau minerale"
              If item names are not readable, write the store or merchant name instead.
              Maximum 10 words. Never write null if you can read any text on the receipt.
            """;

    @Value("${groq.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ReceiptScanService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public ReceiptScanDto scan(String base64Image) {
        try {
            String key = normalizedKey();
            if (key == null || key.isBlank()) {
                return new ReceiptScanDto("other", "EXPENSE", null, null, null);
            }

            Map<String, Object> textBlock = new HashMap<>();
            textBlock.put("type", "text");
            textBlock.put("text", PROMPT);

            Map<String, Object> imageUrl = new HashMap<>();
            imageUrl.put("url", base64Image);

            Map<String, Object> imageBlock = new HashMap<>();
            imageBlock.put("type", "image_url");
            imageBlock.put("image_url", imageUrl);

            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", List.of(textBlock, imageBlock));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", VISION_MODEL);
            requestBody.put("messages", List.of(userMessage));
            requestBody.put("max_tokens", 300);
            requestBody.put("temperature", 0.1);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(key);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(GROQ_URL, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return new ReceiptScanDto("other", "EXPENSE", null, null, null);
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            String raw = root.path("choices").path(0).path("message").path("content").asText("");

            return parseJson(raw);

        } catch (Exception e) {
            System.err.println("ReceiptScanService error: " + e.getMessage());
            return new ReceiptScanDto("other", "EXPENSE", null, null, null);
        }
    }

    private ReceiptScanDto parseJson(String raw) {
        System.out.println("=== RECEIPT SCAN RAW ===\n" + raw + "\n===");
        try {
            // Strip markdown code fences and any leading/trailing prose
            String jsonStr = raw.trim().replaceAll("(?s)```(?:json)?\\s*", "").trim();
            int start = jsonStr.indexOf('{');
            int end   = jsonStr.lastIndexOf('}');
            if (start < 0 || end <= start) {
                System.err.println("ReceiptScanService: no JSON object in response");
                return new ReceiptScanDto("other", "EXPENSE", null, null, null);
            }
            jsonStr = jsonStr.substring(start, end + 1);
            System.out.println("=== RECEIPT SCAN JSON ===\n" + jsonStr + "\n===");

            JsonNode json = objectMapper.readTree(jsonStr);

            String category = sanitizeCategory(json.path("category").asText("other"));

            String rawType = json.path("type").asText("EXPENSE").toUpperCase().trim();
            String type = "INCOME".equals(rawType) ? "INCOME" : "EXPENSE";

            // Amount: accept number OR quoted string (model often returns "47.50" as a string)
            BigDecimal amount = null;
            JsonNode amountNode = json.path("amount");
            if (!amountNode.isNull() && !amountNode.isMissingNode()) {
                if (amountNode.isNumber()) {
                    amount = amountNode.decimalValue();
                } else {
                    // Model returned amount as a quoted string — strip non-numeric chars and parse
                    String amtStr = amountNode.asText("").replaceAll("[^0-9.,]", "").replace(",", ".");
                    // Handle cases like "1.234,56" (European thousands separator)
                    if (amtStr.matches("\\d+\\.\\d{3},\\d+")) {
                        amtStr = amtStr.replace(".", "").replace(",", ".");
                    }
                    if (!amtStr.isBlank()) {
                        try { amount = new BigDecimal(amtStr); } catch (Exception ignored) {}
                    }
                }
            }
            System.out.println("=== RECEIPT SCAN amount=" + amount + " ===");

            String date = null;
            String rawDate = json.path("date").asText("");
            if (!rawDate.isBlank() && !rawDate.equals("null") && rawDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                date = rawDate;
            }

            String description = null;
            String rawDesc = json.path("description").asText("");
            if (!rawDesc.isBlank() && !rawDesc.equals("null")) {
                description = rawDesc.length() > 100 ? rawDesc.substring(0, 100) : rawDesc;
            }
            System.out.println("=== RECEIPT SCAN description=" + description + " ===");

            return new ReceiptScanDto(category, type, amount, date, description);

        } catch (Exception e) {
            System.err.println("ReceiptScanService JSON parse error: " + e.getMessage());
            return new ReceiptScanDto("other", "EXPENSE", null, null, null);
        }
    }

    private String sanitizeCategory(String raw) {
        List<String> valid = List.of(
                "groceries", "food", "transport", "health",
                "shopping", "utilities", "entertainment", "salary", "other");
        String cleaned = raw.trim().toLowerCase().replaceAll("[^a-z]", "");
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
