package tn.esprit.projet_pi.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import tn.esprit.projet_pi.entity.Loan;
import tn.esprit.projet_pi.entity.RepaymentSchedule;
import tn.esprit.projet_pi.enums.PaymentStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MLPredictionService {

    private final String FLASK_ML_URL = "http://127.0.0.1:5000";
    private final RestTemplate restTemplate = new RestTemplate();
    private final LoanService loanService;

    public MLPredictionService(LoanService loanService) {
        this.loanService = loanService;
    }

    public Map<String, Object> predictLoan(Long loanId) {
        try {
            Loan loan = loanService.getLoanById(loanId);

            List<RepaymentSchedule> schedule = loanService.getScheduleByLoan(loanId);
            long overdueCount = schedule.stream()
                    .filter(s -> s.getStatus() == PaymentStatus.OVERDUE)
                    .count();

            Map<String, Object> body = new HashMap<>();
            body.put("amount", loan.getPrincipalAmount().doubleValue());
            body.put("duration", loan.getDurationMonths());
            body.put("riskScore", loan.getRiskScore() != null ? loan.getRiskScore() : 0);
            body.put("overdueCount", overdueCount);
            body.put("defaultCount", 0);
            body.put("loanType", loan.getLoanType().toString().toLowerCase());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    FLASK_ML_URL + "/full-analysis", request, Map.class);
            return response.getBody();

        } catch (Exception e) {
            return Map.of("error", "ML service unavailable: " + e.getMessage());
        }
    }
}