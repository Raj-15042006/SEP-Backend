package com.passport.audit;

import com.passport.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Ledger", description = "Cryptographic hash-chained tamper-evident audit trail endpoints")
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get recent audit ledger entries")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getRecent() {
        return ResponseEntity.ok(ApiResponse.ok(auditService.getRecentLogs()));
    }

    @GetMapping("/resource/{resourceId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get audit history for specific resource ID")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getForResource(@PathVariable String resourceId) {
        return ResponseEntity.ok(ApiResponse.ok(auditService.getLogsForResource(resourceId)));
    }

    @GetMapping("/verify-chain")
    @Operation(summary = "Cryptographically verify entire SHA-256 audit ledger hash chain")
    public ResponseEntity<ApiResponse<AuditService.ChainVerificationReport>> verifyChain() {
        return ResponseEntity.ok(ApiResponse.ok(auditService.verifyLedgerChain()));
    }
}
