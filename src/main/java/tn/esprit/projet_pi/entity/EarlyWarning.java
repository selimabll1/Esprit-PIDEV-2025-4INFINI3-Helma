package tn.esprit.projet_pi.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.projet_pi.enums.EarlyWarningStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "early_warnings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EarlyWarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long loanId;
    private Long userId;
    private Double combinedScore;
    private Double mlScore;
    private Double markovScore;
    private Double mcScore;

    @Column(length = 3000)
    private String aiExplanation;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private EarlyWarningStatus status;
}
