package com.passport.verification;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Verification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "evidence_id", nullable = false)
    private UUID evidenceId;

    @Column(name = "verifier_id", nullable = false)
    private UUID verifierId;

    @Column(nullable = false)
    private String decision; // VERIFIED, CHANGES_REQUESTED, REJECTED

    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    @Column(name = "max_score", nullable = false)
    private Integer maxScore;

    @Column(nullable = false)
    private Integer percentage;

    @Column(name = "competency_level", nullable = false)
    private String competencyLevel; // BEGINNER, DEVELOPING, INTERMEDIATE, ADVANCED, EXPERT

    @Column(columnDefinition = "TEXT")
    private String comments;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
