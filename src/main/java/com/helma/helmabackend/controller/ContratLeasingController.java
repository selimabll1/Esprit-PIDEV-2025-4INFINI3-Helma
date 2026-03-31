package com.helma.helmabackend.controller;

import com.helma.helmabackend.entity.ContratLeasing;
import com.helma.helmabackend.entity.StatutContrat;
import com.helma.helmabackend.service.ContratLeasingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contrats")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ContratLeasingController {

    private final ContratLeasingService contratLeasingService;

    @PostMapping
    public ResponseEntity<ContratLeasing> create(@Valid @RequestBody ContratLeasing contrat) {
        ContratLeasing created = contratLeasingService.create(contrat);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContratLeasing> update(@PathVariable Long id, @Valid @RequestBody ContratLeasing contrat) {
        ContratLeasing updated = contratLeasingService.update(id, contrat);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        contratLeasingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContratLeasing> findById(@PathVariable Long id) {
        ContratLeasing contrat = contratLeasingService.findById(id);
        return ResponseEntity.ok(contrat);
    }

    @GetMapping
    public ResponseEntity<List<ContratLeasing>> findAll() {
        List<ContratLeasing> contrats = contratLeasingService.findAll();
        return ResponseEntity.ok(contrats);
    }

    @GetMapping("/demande/{demandeId}")
    public ResponseEntity<ContratLeasing> findByDemandeId(@PathVariable Long demandeId) {
        ContratLeasing contrat = contratLeasingService.findByDemandeId(demandeId);
        return ResponseEntity.ok(contrat);
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<ContratLeasing>> findByStatut(@PathVariable StatutContrat statut) {
        List<ContratLeasing> contrats = contratLeasingService.findByStatut(statut);
        return ResponseEntity.ok(contrats);
    }

    @PostMapping("/{id}/envoyer-contrat")
    public ResponseEntity<String> envoyerContrat(
            @PathVariable Long id,
            @RequestParam String email,
            @RequestParam String nomClient) {
        contratLeasingService.genererEtEnvoyerContrat(id, email, nomClient);
        return ResponseEntity.ok("Contrat généré et envoyé à : " + email);
    }


}

