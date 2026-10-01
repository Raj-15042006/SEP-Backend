package com.passport.rubric;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "rubric_scores")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RubricScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "verification_id", nullable = false)
    private UUID verificationId;

    @Column(nullable = false)
    private String criterion;

    @Column(name = "assigned_score", nullable = false)
    private Integer assignedScore;

    @Column(name = "max_score", nullable = false)
    private Integer maxScore;

    @Column(name = "criterion_comment")
    private String criterionComment;
}
