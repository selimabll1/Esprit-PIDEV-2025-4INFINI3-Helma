package tn.esprit.projet_pi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.esprit.projet_pi.dto.response.AIDecisionResponseDTO;
import tn.esprit.projet_pi.entity.Loan;
import tn.esprit.projet_pi.repository.LoanRepository;
import tn.esprit.projet_pi.repository.RepaymentScheduleRepository;

@Service
public class AIDecisionService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    @Value("${groq.model}")
    private String groqModel;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private RepaymentScheduleRepository repaymentScheduleRepository;

    public AIDecisionResponseDTO analyzeCredit(Long loanId) {
        // 1. Récupérer le Loan
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        try {
            // 2. Récupérer les données supplémentaires
            long overdueCount = repaymentScheduleRepository.countOverdueByLoanId(loanId);
            long defaultCount = loanRepository.countDefaultedLoansByUser(loan.getUserId());

            // 3. Construire le prompt
            String prompt = String.format(
                    "You are an AI Credit Decision Agent for HELMA,\n" +
                    "a microfinance platform for young people in Tunisia.\n" +
                    "Analyze this loan and respond ONLY in this exact JSON format\n" +
                    "with no extra text:\n" +
                    "{\n" +
                    "  \"decision\": \"APPROVE or REJECT or MANUAL_REVIEW\",\n" +
                    "  \"confidence\": \"percentage like 87%%\",\n" +
                    "  \"explanation\": \"short explanation in French max 2 sentences\"\n" +
                    "}\n" +
                    "Loan data:\n" +
                    "- Amount: %s TND\n" +
                    "- Duration: %s months\n" +
                    "- Type: %s\n" +
                    "- Risk Score: %s / 100\n" +
                    "- Interest Rate: %s%%\n" +
                    "- Overdue payments: %s\n" +
                    "- Past defaults: %s\n" +
                    "Rules:\n" +
                    "- APPROVE if riskScore < 40 and defaultCount == 0\n" +
                    "- REJECT if riskScore > 70 or defaultCount > 1\n" +
                    "- MANUAL_REVIEW otherwise",
                    loan.getPrincipalAmount(),
                    loan.getDurationMonths(),
                    loan.getLoanType(),
                    loan.getRiskScore(),
                    loan.getInterestRate(),
                    overdueCount,
                    defaultCount
            );

            // 4. Appeler l'API Groq avec RestTemplate
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            String apiKey = groqApiKey == null ? "" : groqApiKey.trim();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode bodyNode = objectMapper.createObjectNode();
            bodyNode.put("model", groqModel);

            ArrayNode messages = objectMapper.createArrayNode();
            ObjectNode message = objectMapper.createObjectNode();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);

            bodyNode.set("messages", messages);
            bodyNode.put("max_tokens", 200);
            bodyNode.put("temperature", 0.3);

            String body = objectMapper.writeValueAsString(bodyNode);

            ResponseEntity<String> response = restTemplate.exchange(
                    groqApiUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            // 5. Parser la réponse
            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.get("choices").get(0)
                    .get("message").get("content").asText();

            // Nettoyer le contenu si nécessaire (retirer les backticks markdown)
            content = content.trim();
            if (content.startsWith("```")) {
                content = content.replaceAll("```[a-zA-Z]*\\n?", "").replace("```", "").trim();
            }

            JsonNode aiResult = objectMapper.readTree(content);
            String decision = aiResult.get("decision").asText();
            String confidence = aiResult.get("confidence").asText();
            String explanation = aiResult.get("explanation").asText();

            // 6. Retourner AIDecisionResponseDTO
            return new AIDecisionResponseDTO(
                    decision,
                    confidence,
                    explanation,
                    loan.getRiskScore(),
                    loan.getInterestRate()
            );

        } catch (Exception e) {
            // Log pour voir l'erreur exacte
            System.err.println("GROQ ERROR: " + e.getMessage());
            e.printStackTrace();

            return new AIDecisionResponseDTO(
                    "MANUAL_REVIEW",
                    "0%",
                    "Analyse IA indisponible, revue manuelle requise",
                    loan.getRiskScore(),
                    loan.getInterestRate()
            );

        }
    }
}
