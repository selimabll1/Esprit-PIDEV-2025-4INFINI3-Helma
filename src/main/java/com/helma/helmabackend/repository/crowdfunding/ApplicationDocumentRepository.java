package com.helma.helmabackend.repository.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.ApplicationDocument;
import com.helma.helmabackend.entity.crowdfunding.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocument, Long> {

    List<ApplicationDocument> findByApplicationRaiseId(Long applicationRaiseId);

    Optional<ApplicationDocument> findByApplicationRaiseIdAndDocType(Long applicationRaiseId, DocumentType docType);

    boolean existsByApplicationRaiseIdAndDocType(Long applicationRaiseId, DocumentType docType);

    void deleteByApplicationRaiseId(Long applicationRaiseId);
}