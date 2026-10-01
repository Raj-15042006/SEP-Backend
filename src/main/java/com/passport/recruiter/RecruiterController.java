package com.passport.recruiter;

import com.passport.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recruiter")
@RequiredArgsConstructor
@Tag(name = "Recruiter Talent Discovery", description = "Endpoints for faceted candidate search, role benchmarks, explainable match scores, and candidate comparison")
public class RecruiterController {

    private final RecruiterService recruiterService;

    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Get all target benchmark job roles")
    public ResponseEntity<ApiResponse<List<JobRole>>> getRoles() {
        return ResponseEntity.ok(ApiResponse.ok(recruiterService.getAllJobRoles()));
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Faceted candidate talent search with verified evidence filters")
    public ResponseEntity<ApiResponse<List<RecruiterService.CandidateSummaryDto>>> search(
        @RequestBody(required = false) RecruiterService.SearchCriteria criteria
    ) {
        if (criteria == null) {
            criteria = new RecruiterService.SearchCriteria();
        }
        return ResponseEntity.ok(ApiResponse.ok(recruiterService.searchCandidates(criteria)));
    }

    @GetMapping("/candidate/{studentId}/readiness")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Get explainable role readiness match breakdown & gap diagnostics")
    public ResponseEntity<ApiResponse<RecruiterService.DetailedReadinessDto>> getReadiness(
        @PathVariable UUID studentId,
        @RequestParam(required = false) UUID roleId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(recruiterService.calculateRoleReadiness(studentId, roleId)));
    }

    @PostMapping("/compare")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Compare multiple candidates side-by-side against a target role benchmark")
    public ResponseEntity<ApiResponse<RecruiterService.ComparisonReportDto>> compare(
        @RequestParam(required = false) UUID roleId,
        @RequestBody List<UUID> candidateIds
    ) {
        return ResponseEntity.ok(ApiResponse.ok(recruiterService.compareCandidates(candidateIds, roleId)));
    }
}
