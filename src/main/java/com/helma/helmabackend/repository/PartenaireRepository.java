package com.helma.helmabackend.repository;

import com.helma.helmabackend.entity.Partenaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartenaireRepository extends JpaRepository<Partenaire, Long> {
    List<Partenaire> findByActif(Boolean actif);
    List<Partenaire> findByType(String type);
}

