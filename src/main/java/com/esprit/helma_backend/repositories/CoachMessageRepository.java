package com.esprit.helma_backend.repositories;

import com.esprit.helma_backend.entities.CoachMessage;
import com.esprit.helma_backend.entities.CoachSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoachMessageRepository extends JpaRepository<CoachMessage, Long> {

    List<CoachMessage> findTop10BySessionOrderByCreatedAtAsc(CoachSession session);

    List<CoachMessage> findBySessionOrderByCreatedAtAsc(CoachSession session);
}