package com.passport.verification;

import com.passport.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/verification")
@RequiredArgsConstructor
@Tag(name = "Faculty Verification & Rubrics", description = "Endpoints for academic verification decisions and rubric scoring")
public class VerificationController {

    private final VerificationService verificationService;

    @PostMapping("/evaluate")
    @PreAuthorize("hasRole('VERIFIER') or hasRole('ADMIN')")
    @Operation(summary = "Submit faculty rubric evaluation decision")
    public ResponseEntity<ApiResponse<Verification>> evaluate(
        @RequestBody VerificationService.EvaluationRequest request
    ) {
        Verification verification = verificationService.evaluate(request);
        return ResponseEntity.ok(ApiResponse.ok("Evaluation completed", verification));
    }

    @GetMapping("/evidence/{evidenceId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'VERIFIER', 'RECRUITER', 'ADMIN')")
    @Operation(summary = "Get verification details for evidence")
    public ResponseEntity<ApiResponse<Verification>> getForEvidence(@PathVariable UUID evidenceId) {
        return ResponseEntity.ok(ApiResponse.ok(verificationService.getVerificationForEvidence(evidenceId)));
    }
}
