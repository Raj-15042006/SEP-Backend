package com.passport.portfolio;

import com.passport.auth.User;
import com.passport.auth.UserRepository;
import com.passport.common.ApiResponse;
import com.passport.evidence.Evidence;
import com.passport.evidence.EvidenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
@Tag(name = "Public Student Portfolio", description = "Endpoints for certified public candidate dossiers")
public class PortfolioController {

    private final EvidenceService evidenceService;
    private final UserRepository userRepository;

    @Data
    @Builder
    public static class PublicDossierDto {
        private UUID studentId;
        private String name;
        private String headline;
        private String bio;
        private String college;
        private String degree;
        private Integer gradYear;
        private String department;
        private List<Evidence> verifiedEvidences;
    }

    @GetMapping("/{studentId}")
    @Operation(summary = "Get public verified competency dossier for student")
    public ResponseEntity<ApiResponse<PublicDossierDto>> getPublicDossier(@PathVariable UUID studentId) {
        User student = userRepository.findById(studentId).orElse(null);

        List<Evidence> evidences = evidenceService.getStudentEvidences(studentId);
        List<Evidence> verified = evidences.stream()
            .filter(e -> e.getStatus() != null && e.getStatus().name().equals("VERIFIED"))
            .toList();

        PublicDossierDto dossier = PublicDossierDto.builder()
            .studentId(studentId)
            .name(student != null ? student.getName() : "Verified Student")
            .headline(student != null && student.getHeadline() != null ? student.getHeadline() : "Student Passport Holder")
            .bio(student != null ? student.getBio() : null)
            .college(student != null ? student.getCollege() : "School of Computing & Engineering")
            .degree(student != null ? student.getDegree() : "B.S. Computer Science")
            .department(student != null ? student.getDepartment() : "Computer Science")
            .gradYear(student != null && student.getGradYear() != null ? student.getGradYear() : 2026)
            .verifiedEvidences(verified)
            .build();

        return ResponseEntity.ok(ApiResponse.ok(dossier));
    }
}
