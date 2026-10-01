package com.passport.auth;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String role; // STUDENT, VERIFIER, RECRUITER, ADMIN

    private String department;

    private String headline;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String college;

    private String degree;

    @Column(name = "grad_year")
    private Integer gradYear;

    @Column(name = "organization_name")
    private String organizationName;

    @Column(name = "employee_id")
    private String employeeId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
