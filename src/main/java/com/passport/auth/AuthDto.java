package com.passport.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

public class AuthDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank(message = "Name is required")
        private String name;

        @Email(message = "Valid email is required")
        @NotBlank(message = "Email is required")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;

        @NotBlank(message = "Role is required")
        private String role; // STUDENT, VERIFIER, RECRUITER, ADMIN

        private String department;
        private String headline;
        private String bio;
        private String college;
        private String degree;
        private Integer gradYear;
        private String organizationName;
        private String employeeId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @Email
        @NotBlank(message = "Email is required")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {
        private String token;
        private String tokenType;
        private long expiresIn;
        private UserDto user;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private UUID id;
        private String name;
        private String email;
        private String role;
        private String department;
        private String headline;
        private String bio;
        private String college;
        private String degree;
        private Integer gradYear;
        private String organizationName;
        private String employeeId;
    }
}
