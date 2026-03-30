package tn.esprit.projet_pi.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.Map;

@Service
public class MarkovService {

    private final String FLASK_MARKOV_URL = "http://127.0.0.1:5001";
    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> predictMarkov(Long loanId) {
        try {
            // 🔥 Body JSON (tu peux améliorer plus tard avec LoanService)
            Map<String, Object> body = Map.of(
                    "currentState", "GOOD_PAYER",
                    "riskScore", 50,
                    "overdueCount", 1,
                    "monthsAhead", 3
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    FLASK_MARKOV_URL + "/markov-predict",
                    request,
                    Map.class
            );

            return response.getBody();

        } catch (Exception e) {
            return Map.of("error", "Markov service unavailable: " + e.getMessage());
        }
    }
}