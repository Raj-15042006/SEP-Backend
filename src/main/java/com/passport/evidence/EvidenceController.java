package com.passport.evidence;

import com.passport.common.ApiResponse;
import com.passport.common.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/evidence")
@RequiredArgsConstructor
@Tag(name = "Evidence Management", description = "Endpoints for uploading, tracking, and retrieving competency evidence")
public class EvidenceController {

    private final EvidenceService evidenceService;

    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Submit new competency evidence artefact")
    public ResponseEntity<ApiResponse<Evidence>> submit(
        @RequestParam UUID skillId,
        @RequestParam String title,
        @RequestParam String type,
        @RequestParam String description,
        @RequestParam(required = false) String repoUrl,
        @RequestParam(required = false) String liveUrl,
        @RequestParam(required = false) String techStack,
        @RequestParam(required = false) String issuingOrg,
        @RequestPart(required = false) MultipartFile file
    ) {
        Evidence created = evidenceService.submitEvidence(
            skillId, title, type, description, repoUrl, liveUrl, techStack, issuingOrg, file
        );
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Evidence submitted successfully", created));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    @Operation(summary = "Update competency evidence artefact")
    public ResponseEntity<ApiResponse<Evidence>> update(
        @PathVariable UUID id,
        @RequestParam(required = false) String title,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) String repoUrl,
        @RequestParam(required = false) String liveUrl,
        @RequestParam(required = false) String techStack,
        @RequestPart(required = false) MultipartFile file
    ) {
        Evidence updated = evidenceService.updateEvidence(
            id, title, description, repoUrl, liveUrl, techStack, file
        );
        return ResponseEntity.ok(ApiResponse.ok("Evidence updated successfully", updated));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'VERIFIER', 'RECRUITER', 'ADMIN')")
    @Operation(summary = "Get all evidence artefacts submitted by student (Enforcing ownership privacy)")
    public ResponseEntity<ApiResponse<List<Evidence>>> getStudentEvidences(@PathVariable UUID studentId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        boolean isOwner = currentUserId.equals(studentId);
        boolean isReviewerOrAdmin = SecurityUtils.hasRole("ROLE_VERIFIER")
                || SecurityUtils.hasRole("ROLE_RECRUITER")
                || SecurityUtils.hasRole("ROLE_ADMIN");

        if (!isOwner && !isReviewerOrAdmin) {
            throw new AccessDeniedException("Access denied: Students can only access their own private evidence submissions.");
        }
        return ResponseEntity.ok(ApiResponse.ok(evidenceService.getStudentEvidences(studentId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'VERIFIER', 'RECRUITER', 'ADMIN')")
    @Operation(summary = "Get single evidence artefact by UUID (Enforcing unverified evidence privacy)")
    public ResponseEntity<ApiResponse<Evidence>> getEvidence(@PathVariable UUID id) {
        Evidence evidence = evidenceService.getEvidenceById(id);
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        boolean isOwner = evidence.getStudentId().equals(currentUserId);
        boolean isVerified = evidence.getStatus() == EvidenceStatus.VERIFIED;
        boolean isReviewerOrAdmin = SecurityUtils.hasRole("ROLE_VERIFIER")
                || SecurityUtils.hasRole("ROLE_RECRUITER")
                || SecurityUtils.hasRole("ROLE_ADMIN");

        if (!isOwner && !isVerified && !isReviewerOrAdmin) {
            throw new AccessDeniedException("Access denied: Unverified draft evidence is private to the submitting student.");
        }
        return ResponseEntity.ok(ApiResponse.ok(evidence));
    }

    @GetMapping("/queue")
    @PreAuthorize("hasRole('VERIFIER') or hasRole('ADMIN')")
    @Operation(summary = "Get faculty verification queue")
    public ResponseEntity<ApiResponse<List<Evidence>>> getQueue(
        @RequestParam(required = false) EvidenceStatus status
    ) {
        return ResponseEntity.ok(ApiResponse.ok(evidenceService.getVerificationQueue(status)));
    }
}
