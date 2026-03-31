package com.helma.helmabackend.controller;

import com.helma.helmabackend.entity.PaiementLeasing;
import com.helma.helmabackend.entity.StatutPaiement;
import com.helma.helmabackend.service.PaiementLeasingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/paiements")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaiementLeasingController {

    private final PaiementLeasingService paiementLeasingService;

    @PostMapping
    public ResponseEntity<PaiementLeasing> create(@Valid @RequestBody PaiementLeasing paiement) {
        PaiementLeasing created = paiementLeasingService.create(paiement);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaiementLeasing> update(@PathVariable Long id, @Valid @RequestBody PaiementLeasing paiement) {
        PaiementLeasing updated = paiementLeasingService.update(id, paiement);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        paiementLeasingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaiementLeasing> findById(@PathVariable Long id) {
        PaiementLeasing paiement = paiementLeasingService.findById(id);
        return ResponseEntity.ok(paiement);
    }

    @GetMapping
    public ResponseEntity<List<PaiementLeasing>> findAll() {
        List<PaiementLeasing> paiements = paiementLeasingService.findAll();
        return ResponseEntity.ok(paiements);
    }

    @GetMapping("/contrat/{contratId}")
    public ResponseEntity<List<PaiementLeasing>> findByContratId(@PathVariable Long contratId) {
        List<PaiementLeasing> paiements = paiementLeasingService.findByContratId(contratId);
        return ResponseEntity.ok(paiements);
    }

    @GetMapping("/statut/{statutPaiement}")
    public ResponseEntity<List<PaiementLeasing>> findByStatutPaiement(@PathVariable StatutPaiement statutPaiement) {
        List<PaiementLeasing> paiements = paiementLeasingService.findByStatutPaiement(statutPaiement);
        return ResponseEntity.ok(paiements);
    }

    @GetMapping("/mois/{mois}")
    public ResponseEntity<List<PaiementLeasing>> findByMois(@PathVariable String mois) {
        List<PaiementLeasing> paiements = paiementLeasingService.findByMois(mois);
        return ResponseEntity.ok(paiements);
    }


}

