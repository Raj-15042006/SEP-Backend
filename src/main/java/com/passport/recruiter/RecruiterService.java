package com.passport.recruiter;

import com.passport.evidence.Evidence;
import com.passport.evidence.EvidenceRepository;
import com.passport.evidence.EvidenceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecruiterService {

    private final JobRoleRepository jobRoleRepository;
    private final EvidenceRepository evidenceRepository;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchCriteria {
        private UUID roleId;
        private List<String> requiredSkills;
        private Boolean verifiedOnly;
        private Integer minMatchScore;
        private Integer minGradYear;
        private Integer maxGradYear;
        private String college;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CandidateSummaryDto {
        private UUID studentId;
        private String name;
        private String headline;
        private String college;
        private Integer gradYear;
        private Integer matchPercentage;
        private Integer verifiedCount;
        private Integer totalCount;
        private List<String> topVerifiedSkills;
        private List<String> competencyGaps;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleBenchmarkSkill {
        private String skill;
        private Double weight;
        private String minLevel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillDiagnostic {
        private String skill;
        private Double weight;
        private String requiredLevel;
        private String candidateLevel;
        private Integer rubricScore;
        private String status; // STRENGTH, COMPETENT, GAP, MISSING
        private String evidenceTitle;
        private String verificationDecision;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailedReadinessDto {
        private UUID studentId;
        private String name;
        private String headline;
        private String college;
        private Integer gradYear;
        private UUID roleId;
        private String roleTitle;
        private Integer overallMatchPercentage;
        private List<SkillDiagnostic> skillDiagnostics;
        private List<String> keyStrengths;
        private List<String> actionableGaps;
        private String aiSynthesis;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonReportDto {
        private UUID roleId;
        private String roleTitle;
        private List<DetailedReadinessDto> candidates;
        private Map<String, Object> benchmarkComparisonMatrix;
    }

    public List<JobRole> getAllJobRoles() {
        return jobRoleRepository.findAll();
    }

    public List<CandidateSummaryDto> searchCandidates(SearchCriteria criteria) {
        // Built-in verified talent pool
        List<CandidateSummaryDto> candidates = getSeedCandidates();

        // Apply filters
        return candidates.stream()
            .filter(c -> {
                if (criteria.getVerifiedOnly() != null && criteria.getVerifiedOnly()) {
                    if (c.getVerifiedCount() == 0) return false;
                }
                if (criteria.getMinMatchScore() != null) {
                    if (c.getMatchPercentage() < criteria.getMinMatchScore()) return false;
                }
                if (criteria.getCollege() != null && !criteria.getCollege().isBlank()) {
                    if (!c.getCollege().toLowerCase().contains(criteria.getCollege().toLowerCase())) return false;
                }
                if (criteria.getRequiredSkills() != null && !criteria.getRequiredSkills().isEmpty()) {
                    boolean hasSkill = criteria.getRequiredSkills().stream()
                        .anyMatch(skill -> c.getTopVerifiedSkills().stream()
                            .anyMatch(ts -> ts.toLowerCase().contains(skill.toLowerCase())));
                    if (!hasSkill) return false;
                }
                return true;
            })
            .sorted(Comparator.comparingInt(CandidateSummaryDto::getMatchPercentage).reversed())
            .collect(Collectors.toList());
    }

    public DetailedReadinessDto calculateRoleReadiness(UUID studentId, UUID roleId) {
        String roleTitle = "Backend Systems Engineer";
        if (roleId != null) {
            roleTitle = jobRoleRepository.findById(roleId)
                .map(JobRole::getTitle)
                .orElse("Backend Systems Engineer");
        }

        List<SkillDiagnostic> diagnostics = List.of(
            SkillDiagnostic.builder()
                .skill("Java")
                .weight(0.35)
                .requiredLevel("Advanced")
                .candidateLevel("Advanced")
                .rubricScore(92)
                .status("STRENGTH")
                .evidenceTitle("Concurrent Hospital Routing Service")
                .verificationDecision("VERIFIED")
                .build(),
            SkillDiagnostic.builder()
                .skill("PostgreSQL")
                .weight(0.25)
                .requiredLevel("Intermediate")
                .candidateLevel("Advanced")
                .rubricScore(90)
                .status("STRENGTH")
                .evidenceTitle("Database Indexing & Partitioning Analysis")
                .verificationDecision("VERIFIED")
                .build(),
            SkillDiagnostic.builder()
                .skill("Spring Boot")
                .weight(0.25)
                .requiredLevel("Intermediate")
                .candidateLevel("Intermediate")
                .rubricScore(88)
                .status("COMPETENT")
                .evidenceTitle("Microservices Gateway & OAuth2 Resource Server")
                .verificationDecision("VERIFIED")
                .build(),
            SkillDiagnostic.builder()
                .skill("Apache Kafka")
                .weight(0.15)
                .requiredLevel("Intermediate")
                .candidateLevel("Beginner")
                .rubricScore(40)
                .status("GAP")
                .evidenceTitle("Event Consumer Prototype (Pending Review)")
                .verificationDecision("PENDING")
                .build()
        );

        int totalWeightScore = (int) Math.round(
            (0.35 * 92) + (0.25 * 90) + (0.25 * 88) + (0.15 * 40)
        );

        return DetailedReadinessDto.builder()
            .studentId(studentId)
            .name("Aarav Sharma")
            .headline("Backend Systems Engineer | Distributed Systems & Java")
            .college("National Institute of Technology")
            .gradYear(2026)
            .roleId(roleId)
            .roleTitle(roleTitle)
            .overallMatchPercentage(totalWeightScore)
            .skillDiagnostics(diagnostics)
            .keyStrengths(List.of(
                "Demonstrated mastery of Java Virtual Threads & Concurrency (92% rubric score)",
                "Solid PostgreSQL relational schema design and query execution plan tuning (90%)",
                "Clean Spring Boot microservices architectural decoupling"
            ))
            .actionableGaps(List.of(
                "Requires verified production proof in distributed Apache Kafka partition rebalancing"
            ))
            .aiSynthesis("Candidate exceeds benchmark requirements for core backend systems and databases. Ready for high-velocity backend engineering roles with minimal ramp-up.")
            .build();
    }

    public ComparisonReportDto compareCandidates(List<UUID> candidateIds, UUID roleId) {
        String roleTitle = "Backend Systems Engineer";
        if (roleId != null) {
            roleTitle = jobRoleRepository.findById(roleId)
                .map(JobRole::getTitle)
                .orElse("Backend Systems Engineer");
        }

        List<DetailedReadinessDto> reportList = new ArrayList<>();
        for (UUID cid : candidateIds) {
            reportList.add(calculateRoleReadiness(cid, roleId));
        }

        Map<String, Object> matrix = new LinkedHashMap<>();
        matrix.put("evaluatedRole", roleTitle);
        matrix.put("candidateCount", candidateIds.size());
        matrix.put("topCandidateId", candidateIds.isEmpty() ? null : candidateIds.get(0));

        return ComparisonReportDto.builder()
            .roleId(roleId)
            .roleTitle(roleTitle)
            .candidates(reportList)
            .benchmarkComparisonMatrix(matrix)
            .build();
    }

    private List<CandidateSummaryDto> getSeedCandidates() {
        return List.of(
            CandidateSummaryDto.builder()
                .studentId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .name("Aarav Sharma")
                .headline("Backend Systems Engineer | Distributed Systems & Java")
                .college("National Institute of Technology")
                .gradYear(2026)
                .matchPercentage(91)
                .verifiedCount(4)
                .totalCount(5)
                .topVerifiedSkills(List.of("Java (92%)", "PostgreSQL (90%)", "Spring Boot (88%)", "Docker (85%)"))
                .competencyGaps(List.of("Apache Kafka"))
                .build(),
            CandidateSummaryDto.builder()
                .studentId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                .name("Priya Patel")
                .headline("Full Stack Engineer | React, Node.js & Cloud")
                .college("Indian Institute of Information Technology")
                .gradYear(2025)
                .matchPercentage(86)
                .verifiedCount(3)
                .totalCount(4)
                .topVerifiedSkills(List.of("React (94%)", "REST API Design (89%)", "TypeScript (86%)"))
                .competencyGaps(List.of("Distributed Databases"))
                .build(),
            CandidateSummaryDto.builder()
                .studentId(UUID.fromString("00000000-0000-0000-0000-000000000003"))
                .name("Rohan Verma")
                .headline("DevOps & Cloud Engineer | Kubernetes & Go")
                .college("BITS Pilani")
                .gradYear(2026)
                .matchPercentage(78)
                .verifiedCount(3)
                .totalCount(5)
                .topVerifiedSkills(List.of("Docker (92%)", "Linux (88%)", "Python (80%)"))
                .competencyGaps(List.of("Java Concurrency", "Spring Data JPA"))
                .build()
        );
    }
}
