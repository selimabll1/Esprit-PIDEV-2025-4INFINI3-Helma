package com.helma.helmabackend.entity.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.enums.CampaignPageStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "campaign_page",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_campaign_page_application", columnNames = "application_raise_id"),
                @UniqueConstraint(name = "uk_campaign_page_slug", columnNames = "slug")
        },
        indexes = {
                @Index(name = "idx_campaign_page_owner", columnList = "owner_user_id"),
                @Index(name = "idx_campaign_page_status", columnList = "status"),
                @Index(name = "idx_campaign_page_slug", columnList = "slug")
        }
)
@Getter
@Setter
public class CampaignPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_raise_id", nullable = false)
    private Long applicationRaiseId;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 280)
    private String subtitle;

    @Column(name = "cover_media_url", length = 1000)
    private String coverMediaUrl;

    @Column(name = "content_json", nullable = false, columnDefinition = "LONGTEXT")
    private String contentJson;

    @Column(name = "style_json", nullable = false, columnDefinition = "LONGTEXT")
    private String styleJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CampaignPageStatus status = CampaignPageStatus.DRAFT;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = CampaignPageStatus.DRAFT;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
        if (status == null) {
            status = CampaignPageStatus.DRAFT;
        }
    }
}
