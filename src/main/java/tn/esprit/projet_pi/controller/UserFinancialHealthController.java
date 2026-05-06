package tn.esprit.projet_pi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.projet_pi.dto.response.FinancialHealthDTO;
import tn.esprit.projet_pi.security.LoanAccessService;
import tn.esprit.projet_pi.service.FinancialHealthService;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserFinancialHealthController {

    private final FinancialHealthService financialHealthService;
    private final LoanAccessService loanAccessService;

    public UserFinancialHealthController(FinancialHealthService financialHealthService,
            LoanAccessService loanAccessService) {
        this.financialHealthService = financialHealthService;
        this.loanAccessService = loanAccessService;
    }

    @GetMapping("/{userId}/financial-health")
    public ResponseEntity<FinancialHealthDTO> getFinancialHealth(@PathVariable Long userId) {
        loanAccessService.assertCanAccessUser(userId);
        return ResponseEntity.ok(financialHealthService.calculateScore(userId));
    }
}
