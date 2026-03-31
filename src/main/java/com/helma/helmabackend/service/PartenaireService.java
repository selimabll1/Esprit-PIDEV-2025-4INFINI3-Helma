package com.helma.helmabackend.service;

import com.helma.helmabackend.entity.Partenaire;
import com.helma.helmabackend.repository.PartenaireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PartenaireService {

    private final PartenaireRepository partenaireRepository;

    public Partenaire create(Partenaire partenaire) {
        return partenaireRepository.save(partenaire);
    }

    public Partenaire update(Long id, Partenaire partenaire) {
        Partenaire existing = findById(id);
        existing.setNom(partenaire.getNom());
        existing.setType(partenaire.getType());
        existing.setContact(partenaire.getContact());
        existing.setActif(partenaire.getActif());
        return partenaireRepository.save(existing);
    }

    public void delete(Long id) {
        partenaireRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Partenaire findById(Long id) {
        return partenaireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Partenaire non trouvé avec l'id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Partenaire> findAll() {
        return partenaireRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Partenaire> findByActif(Boolean actif) {
        return partenaireRepository.findByActif(actif);
    }

    @Transactional(readOnly = true)
    public List<Partenaire> findByType(String type) {
        return partenaireRepository.findByType(type);
    }

    public Partenaire updateActif(Long id, Boolean actif) {
        Partenaire partenaire = findById(id);
        partenaire.setActif(actif);
        return partenaireRepository.save(partenaire);
    }
}

