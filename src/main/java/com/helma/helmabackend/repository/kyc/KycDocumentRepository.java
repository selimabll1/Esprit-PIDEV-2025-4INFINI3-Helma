package com.helma.helmabackend.repository.kyc;

import com.helma.helmabackend.entity.kyc.KycDocument;
import com.helma.helmabackend.entity.kyc.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {
    Optional<KycDocument> findByUserId(Long userId);
    List<KycDocument> findByStatusOrderBySubmittedAtAsc(KycStatus status);
}
