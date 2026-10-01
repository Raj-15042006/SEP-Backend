package com.passport.config;

import com.passport.auth.AuthDto;
import com.passport.auth.AuthService;
import com.passport.auth.UserRepository;
import com.passport.taxonomy.Skill;
import com.passport.taxonomy.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SkillRepository skillRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    @Override
    public void run(String... args) {
        seedSkills();
        seedBaselineUsers();
    }

    private void seedSkills() {
        if (skillRepository.count() == 0) {
            log.info("Initializing taxonomy skills in database...");
            List<Skill> skills = List.of(
                Skill.builder()
                    .name("Distributed Consensus (Raft/Paxos)")
                    .category("Distributed Systems & Cloud")
                    .description("Implementation of leader election, log replication, safety invariants, and cluster membership changes.")
                    .taxonomyVersion(2)
                    .build(),
                Skill.builder()
                    .name("Reactive Streaming & Concurrency")
                    .category("Distributed Systems & Cloud")
                    .description("Backpressure control, asynchronous event loops, and fault-isolated actor systems.")
                    .taxonomyVersion(1)
                    .build(),
                Skill.builder()
                    .name("Design Systems & Token Architecture")
                    .category("Frontend Architecture & Craft")
                    .description("Three-layer token systems, strict accessibility, component invariants, and headless primitives.")
                    .taxonomyVersion(2)
                    .build(),
                Skill.builder()
                    .name("Database Storage Engines (LSM/B-Tree)")
                    .category("Computer Systems & Storage")
                    .description("Write-ahead logging, SSTable compaction, lock-free memory indexing, and crash recovery.")
                    .taxonomyVersion(1)
                    .build(),
                Skill.builder()
                    .name("Kubernetes Operator & Cloud Orchestration")
                    .category("Distributed Systems & Cloud")
                    .description("Custom resource definitions, reconciler loops, service mesh policies, and zero-downtime rolling deploys.")
                    .taxonomyVersion(1)
                    .build(),
                Skill.builder()
                    .name("High-Performance UI Micro-Interactions")
                    .category("Frontend Architecture & Craft")
                    .description("GPU-composited transforms, layout thrashing prevention, 60fps gesture physics, and spring mechanics.")
                    .taxonomyVersion(1)
                    .build()
            );
            skillRepository.saveAll(skills);
            log.info("Successfully seeded {} skills into database taxonomy", skills.size());
        }
    }

    private void seedBaselineUsers() {
        if (userRepository.count() == 0) {
            log.info("Initializing institutional baseline accounts in database...");
            try {
                authService.register(AuthDto.RegisterRequest.builder()
                    .name("Alex Chen")
                    .email("student@passport.edu")
                    .password("password123")
                    .role("STUDENT")
                    .department("Computer Science & Engineering")
                    .degree("B.S. Computer Science")
                    .gradYear(2026)
                    .headline("Systems Engineer & Distributed Infrastructure Builder")
                    .bio("Focused on high-concurrency protocols, storage engines, and verifiable code evidence.")
                    .build());

                authService.register(AuthDto.RegisterRequest.builder()
                    .name("Prof. Marcus Vance")
                    .email("faculty@passport.edu")
                    .password("password123")
                    .role("VERIFIER")
                    .department("Distributed Systems Laboratory")
                    .headline("Faculty Chair of Distributed Systems & Operating Architecture")
                    .build());

                authService.register(AuthDto.RegisterRequest.builder()
                    .name("Sarah Jenkins")
                    .email("recruiter@passport.edu")
                    .password("password123")
                    .role("RECRUITER")
                    .organizationName("Acme Cloud Systems Inc.")
                    .department("Global Engineering Hiring")
                    .headline("Principal Technical Talent Lead")
                    .build());

                authService.register(AuthDto.RegisterRequest.builder()
                    .name("System Administrator")
                    .email("admin@passport.edu")
                    .password("password123")
                    .role("ADMIN")
                    .department("Academic Affairs & Registry")
                    .headline("Chief Academic Credentials Administrator")
                    .build());

                log.info("Baseline institutional users initialized successfully");
            } catch (Exception e) {
                log.warn("Notice: Baseline user creation skipped or already completed: {}", e.getMessage());
            }
        }
    }
}
