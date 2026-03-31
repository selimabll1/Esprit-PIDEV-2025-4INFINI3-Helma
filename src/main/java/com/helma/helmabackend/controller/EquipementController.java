package com.helma.helmabackend.controller;

import com.helma.helmabackend.entity.Equipement;
import com.helma.helmabackend.service.EquipementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipements")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EquipementController {

    private final EquipementService equipementService;

    @PostMapping
    public ResponseEntity<Equipement> create(@Valid @RequestBody Equipement equipement) {
        Equipement created = equipementService.create(equipement);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Equipement> update(@PathVariable Long id, @Valid @RequestBody Equipement equipement) {
        Equipement updated = equipementService.update(id, equipement);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        equipementService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Equipement> findById(@PathVariable Long id) {
        Equipement equipement = equipementService.findById(id);
        return ResponseEntity.ok(equipement);
    }

    @GetMapping
    public ResponseEntity<List<Equipement>> findAll() {
        List<Equipement> equipements = equipementService.findAll();
        return ResponseEntity.ok(equipements);
    }

    @GetMapping("/disponible/{disponible}")
    public ResponseEntity<List<Equipement>> findByDisponible(@PathVariable Boolean disponible) {
        List<Equipement> equipements = equipementService.findByDisponible(disponible);
        return ResponseEntity.ok(equipements);
    }

    @GetMapping("/categorie/{categorie}")
    public ResponseEntity<List<Equipement>> findByCategorie(@PathVariable String categorie) {
        List<Equipement> equipements = equipementService.findByCategorie(categorie);
        return ResponseEntity.ok(equipements);
    }

    @GetMapping("/partenaire/{partenaireId}")
    public ResponseEntity<List<Equipement>> findByPartenaireId(@PathVariable Long partenaireId) {
        List<Equipement> equipements = equipementService.findByPartenaireId(partenaireId);
        return ResponseEntity.ok(equipements);
    }


}

