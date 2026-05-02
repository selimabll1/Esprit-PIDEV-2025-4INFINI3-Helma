package com.helma.helmabackend.entity.crowdfunding;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "campaign_page_document",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_campaign_page_document", columnNames = {"campaign_page_id", "application_document_id"})
        },
        indexes = {
                @Index(name = "idx_campaign_page_document_campaign", columnList = "campaign_page_id"),
                @Index(name = "idx_campaign_page_document_doc", columnList = "application_document_id")
        }
)
@Getter
@Setter
public class CampaignPageDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_page_id", nullable = false)
    private Long campaignPageId;

    @Column(name = "application_document_id", nullable = false)
    private Long applicationDocumentId;

    @Column(length = 160)
    private String label;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}
