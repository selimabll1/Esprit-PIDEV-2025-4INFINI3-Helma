package com.esprit.helma_backend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "risk_cases")
public class RiskCase {

    public enum Status {
        OPEN, RESOLVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK -> users.id (owner of the case)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "risk_level", nullable = false)
    private Integer riskLevel;

    // FK -> users.id (admin assigned), nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_admin_id")
    private User assignedAdmin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @PrePersist
    public void prePersist() {
        if (detectedAt == null) detectedAt = Instant.now();
        if (status == null) status = Status.OPEN;
    }
}