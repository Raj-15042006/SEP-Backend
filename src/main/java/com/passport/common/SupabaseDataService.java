package com.passport.common;

import com.passport.config.SupabaseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class SupabaseDataService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseDataService.class);

    private final SupabaseProperties properties;
    private final RestClient restClient;

    public SupabaseDataService(SupabaseProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getUrl() + "/rest/v1")
                .defaultHeader("apikey", properties.getAnonKey())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAnonKey())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public List<Map<String, Object>> fetchSkills() {
        try {
            log.info("Fetching skill taxonomy from Supabase cloud database...");
            List<Map<String, Object>> result = restClient.get()
                    .uri("/skills?select=*&order=name.asc")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Supabase fetch skills fallback (using local cache): {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> fetchUsers() {
        try {
            log.info("Fetching student and evaluator accounts from Supabase users table...");
            List<Map<String, Object>> result = restClient.get()
                    .uri("/users?select=*&order=name.asc")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Supabase fetch users fallback: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<String, Object> fetchUserByEmail(String email) {
        try {
            List<Map<String, Object>> result = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/users")
                            .queryParam("email", "eq." + email.toLowerCase().trim())
                            .queryParam("select", "*")
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            return (result != null && !result.isEmpty()) ? result.get(0) : null;
        } catch (Exception e) {
            log.warn("Supabase fetch user by email fallback: {}", e.getMessage());
            return null;
        }
    }

    public List<Map<String, Object>> fetchEvidence(String studentId) {
        try {
            log.info("Fetching evidence records from Supabase for student: {}", studentId);
            String uri = (studentId != null && !studentId.isBlank())
                    ? "/evidence?student_id=eq." + studentId + "&select=*&order=submitted_at.desc"
                    : "/evidence?select=*&order=submitted_at.desc";

            List<Map<String, Object>> result = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Supabase fetch evidence fallback: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> fetchAuditLogs() {
        try {
            log.info("Fetching cryptographic audit ledger logs from Supabase...");
            List<Map<String, Object>> result = restClient.get()
                    .uri("/audit_logs?select=*&order=timestamp.desc&limit=50")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Supabase fetch audit logs fallback: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean upsertUser(Map<String, Object> userData) {
        try {
            restClient.post()
                    .uri("/users")
                    .header("Prefer", "resolution=merge-duplicates")
                    .body(List.of(userData))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully synced user profile to Supabase users table: {}", userData.get("email"));
            return true;
        } catch (Exception e) {
            log.warn("Supabase upsert user fallback: {}", e.getMessage());
            return false;
        }
    }

    public boolean insertAuditLog(Map<String, Object> logEntry) {
        try {
            restClient.post()
                    .uri("/audit_logs")
                    .body(List.of(logEntry))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("Supabase insert audit log fallback: {}", e.getMessage());
            return false;
        }
    }
}
