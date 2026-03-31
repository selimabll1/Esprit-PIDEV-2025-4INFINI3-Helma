package com.helma.helmabackend.controller;

import com.helma.helmabackend.entity.DemandeLeasing;
import com.helma.helmabackend.entity.StatutDemande;
import com.helma.helmabackend.service.DemandeLeasingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/demandes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DemandeLeasingController {

    private final DemandeLeasingService demandeLeasingService;

    @PostMapping
    public ResponseEntity<DemandeLeasing> create(@Valid @RequestBody DemandeLeasing demande) {
        DemandeLeasing created = demandeLeasingService.create(demande);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DemandeLeasing> update(@PathVariable Long id, @Valid @RequestBody DemandeLeasing demande) {
        DemandeLeasing updated = demandeLeasingService.update(id, demande);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        demandeLeasingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DemandeLeasing> findById(@PathVariable Long id) {
        DemandeLeasing demande = demandeLeasingService.findById(id);
        return ResponseEntity.ok(demande);
    }

    @GetMapping
    public ResponseEntity<List<DemandeLeasing>> findAll() {
        List<DemandeLeasing> demandes = demandeLeasingService.findAll();
        return ResponseEntity.ok(demandes);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DemandeLeasing>> findByUserId(@PathVariable Long userId) {
        List<DemandeLeasing> demandes = demandeLeasingService.findByUserId(userId);
        return ResponseEntity.ok(demandes);
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<DemandeLeasing>> findByStatut(@PathVariable StatutDemande statut) {
        List<DemandeLeasing> demandes = demandeLeasingService.findByStatut(statut);
        return ResponseEntity.ok(demandes);
    }

    @GetMapping("/equipement/{equipementId}")
    public ResponseEntity<List<DemandeLeasing>> findByEquipementId(@PathVariable Long equipementId) {
        List<DemandeLeasing> demandes = demandeLeasingService.findByEquipementId(equipementId);
        return ResponseEntity.ok(demandes);
    }


}

