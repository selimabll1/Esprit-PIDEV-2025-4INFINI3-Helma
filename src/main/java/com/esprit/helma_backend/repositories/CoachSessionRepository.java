package com.esprit.helma_backend.repositories;

import com.esprit.helma_backend.entities.CoachSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface CoachSessionRepository extends JpaRepository<CoachSession, Long> {

    Optional<CoachSession> findTopByUserIdOrderByLastActivityAtDesc(Long userId);

    Optional<CoachSession> findByUserIdAndLastActivityAtAfter(Long userId, Instant since);
}