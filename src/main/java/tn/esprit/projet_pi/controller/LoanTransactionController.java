package tn.esprit.projet_pi.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.projet_pi.entity.LoanTransaction;
import tn.esprit.projet_pi.service.LoanTransactionService;

import java.util.List;

@RestController
@RequestMapping("/api/loan-transactions") // 🔥 cohérent avec reste API
@CrossOrigin("*")
public class LoanTransactionController {

    private final LoanTransactionService service;

    public LoanTransactionController(LoanTransactionService service) {
        this.service = service;
    }

    // 🔥 CREATE
    @PostMapping
    public ResponseEntity<LoanTransaction> create(@Valid @RequestBody LoanTransaction transaction) {
        LoanTransaction saved = service.save(transaction);
        return ResponseEntity.status(201).body(saved);
    }

    // 🔥 READ ALL
    @GetMapping
    public ResponseEntity<List<LoanTransaction>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    // 🔥 READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<LoanTransaction> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    // 🔥 DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build(); // 204
    }
}