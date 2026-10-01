package com.passport.auth;

import com.passport.common.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & Identity Gateway", description = "Endpoints for user registration, stakeholder login, and session validation")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register new stakeholder (Student, Verifier)")
    public ResponseEntity<AuthDto.AuthResponse> register(@Valid @RequestBody AuthDto.RegisterRequest req) {
        AuthDto.AuthResponse response = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user with email and password")
    public ResponseEntity<AuthDto.AuthResponse> login(@Valid @RequestBody AuthDto.LoginRequest req) {
        AuthDto.AuthResponse response = authService.login(req);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<AuthDto.UserDto> getCurrentUserSession() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        AuthDto.UserDto profile = authService.getCurrentUserProfile(currentUserId);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/me/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get user profile by user ID (Subject to ownership and role authorization)")
    public ResponseEntity<AuthDto.UserDto> getCurrentUser(@PathVariable UUID userId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAuthorized = currentUserId.equals(userId)
                || SecurityUtils.hasRole("ROLE_ADMIN")
                || SecurityUtils.hasRole("ROLE_RECRUITER");

        if (!isAuthorized) {
            throw new AccessDeniedException("Access denied: You are not authorized to view another user's private profile details.");
        }

        AuthDto.UserDto profile = authService.getCurrentUserProfile(userId);
        return ResponseEntity.ok(profile);
    }
}
