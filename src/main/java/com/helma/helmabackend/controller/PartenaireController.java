package com.helma.helmabackend.controller;

import com.helma.helmabackend.entity.Partenaire;
import com.helma.helmabackend.service.PartenaireService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/partenaires")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PartenaireController {

    private final PartenaireService partenaireService;

    @PostMapping
    public ResponseEntity<Partenaire> create(@Valid @RequestBody Partenaire partenaire) {
        Partenaire created = partenaireService.create(partenaire);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Partenaire> update(@PathVariable Long id, @Valid @RequestBody Partenaire partenaire) {
        Partenaire updated = partenaireService.update(id, partenaire);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        partenaireService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Partenaire> findById(@PathVariable Long id) {
        Partenaire partenaire = partenaireService.findById(id);
        return ResponseEntity.ok(partenaire);
    }

    @GetMapping
    public ResponseEntity<List<Partenaire>> findAll() {
        List<Partenaire> partenaires = partenaireService.findAll();
        return ResponseEntity.ok(partenaires);
    }

    @GetMapping("/actif/{actif}")
    public ResponseEntity<List<Partenaire>> findByActif(@PathVariable Boolean actif) {
        List<Partenaire> partenaires = partenaireService.findByActif(actif);
        return ResponseEntity.ok(partenaires);
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Partenaire>> findByType(@PathVariable String type) {
        List<Partenaire> partenaires = partenaireService.findByType(type);
        return ResponseEntity.ok(partenaires);
    }


}
