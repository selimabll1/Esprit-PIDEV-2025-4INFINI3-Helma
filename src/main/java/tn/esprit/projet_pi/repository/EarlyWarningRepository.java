package tn.esprit.projet_pi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tn.esprit.projet_pi.entity.EarlyWarning;

import java.util.List;

public interface EarlyWarningRepository extends JpaRepository<EarlyWarning, Long> {

    @Query("SELECT e FROM EarlyWarning e WHERE e.combinedScore > 0.6 ORDER BY e.createdAt DESC")
    List<EarlyWarning> findRiskyWarnings();
}
