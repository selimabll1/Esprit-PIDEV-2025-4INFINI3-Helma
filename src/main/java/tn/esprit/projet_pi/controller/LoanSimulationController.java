package tn.esprit.projet_pi.controller;

import jakarta.validation.Valid; // 🔥 IMPORTANT
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.projet_pi.dto.request.LoanSimulationDTO;
import tn.esprit.projet_pi.service.LoanSimulationService;

import java.util.Map;

@RestController
@RequestMapping("/api/loans/simulate")
@CrossOrigin(origins = "*")
public class LoanSimulationController {

    private final LoanSimulationService loanSimulationService;

    public LoanSimulationController(LoanSimulationService loanSimulationService) {
        this.loanSimulationService = loanSimulationService;
    }

    // 🔥 SIMULATION (VALIDATION ICI)
    @PostMapping
    public ResponseEntity<Map<String, Object>> simulate(@Valid @RequestBody LoanSimulationDTO dto) {

        Map<String, Object> result = loanSimulationService.simulate(dto);

        return ResponseEntity.ok(result);
    }
}