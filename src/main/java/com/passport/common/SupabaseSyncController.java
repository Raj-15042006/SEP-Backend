package com.passport.common;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Administrative Database Synchronization Controller
 * Restricted strictly to system administrators (CWE-200 / CWE-284).
 */
@RestController
@RequestMapping("/api/v1/sync")
@PreAuthorize("hasRole('ADMIN')")
public class SupabaseSyncController {

    private final SupabaseDataService supabaseDataService;

    public SupabaseSyncController(SupabaseDataService supabaseDataService) {
        this.supabaseDataService = supabaseDataService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSyncStatus() {
        Map<String, Object> status = new HashMap<>();
        List<Map<String, Object>> skills = supabaseDataService.fetchSkills();
        status.put("connected", true);
        status.put("provider", "Supabase PostgreSQL PostgREST Engine");
        status.put("fetchedSkillsCount", skills.size());
        status.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(status);
    }

    @GetMapping("/skills")
    public ResponseEntity<List<Map<String, Object>>> getSkills() {
        return ResponseEntity.ok(supabaseDataService.fetchSkills());
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getUsers() {
        return ResponseEntity.ok(supabaseDataService.fetchUsers());
    }

    @GetMapping("/evidence")
    public ResponseEntity<List<Map<String, Object>>> getEvidence(@RequestParam(required = false) String studentId) {
        return ResponseEntity.ok(supabaseDataService.fetchEvidence(studentId));
    }

    @GetMapping("/audit")
    public ResponseEntity<List<Map<String, Object>>> getAuditLogs() {
        return ResponseEntity.ok(supabaseDataService.fetchAuditLogs());
    }
}
