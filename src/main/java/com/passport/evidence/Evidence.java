package com.passport.evidence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evidence")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Evidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "skill_id", nullable = false)
    private UUID skillId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String type; // PROJECT, CERTIFICATE, INTERNSHIP, PUBLICATION, OPEN_SOURCE

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_ref")
    private String fileRef;

    @Column(name = "repo_url")
    private String repoUrl;

    @Column(name = "live_url")
    private String liveUrl;

    @Column(name = "tech_stack")
    private String techStack;

    @Column(name = "issuing_org")
    private String issuingOrg;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private EvidenceStatus status = EvidenceStatus.SUBMITTED;

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;
}
