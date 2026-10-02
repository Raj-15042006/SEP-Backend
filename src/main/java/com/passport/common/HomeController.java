package com.passport.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> index() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Skills Evidence Passport API",
            "documentation", "/swagger-ui.html",
            "healthCheck", "/actuator/health",
            "timestamp", System.currentTimeMillis()
        ));
    }
}
