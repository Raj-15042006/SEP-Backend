package com.passport.recruiter;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_roles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String department;

    @Column(name = "required_skills", nullable = false, columnDefinition = "jsonb")
    private String requiredSkills;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
