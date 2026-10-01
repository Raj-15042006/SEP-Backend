package com.passport.auth;

import com.passport.common.Audited;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    @org.springframework.beans.factory.annotation.Value("${passport.security.jwt-secret:passport-institutional-secret-key-2026-sha256-signed}")
    private String hmacSecret = "passport-institutional-secret-key-2026-sha256-signed";

    private static final String SALT = "SEP_SECURE_SALT_v1_";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    public static final long TOKEN_VALIDITY_MS = 24 * 60 * 60 * 1000L; // 24 hours

    @Value
    public static class TokenClaims {
        UUID userId;
        String email;
        String role;
        long issuedAt;
    }

    @Transactional
    @Audited(action = "USER_REGISTERED", resourceType = "user")
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest req) {
        if (req.getEmail() == null || !EMAIL_PATTERN.matcher(req.getEmail().trim()).matches()) {
            throw new IllegalArgumentException("Invalid institutional email address format.");
        }

        if (userRepository.existsByEmail(req.getEmail().toLowerCase().trim())) {
            throw new IllegalArgumentException("User with email " + req.getEmail() + " already exists.");
        }

        if (req.getPassword() == null || req.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }

        String rawRole = req.getRole() != null ? req.getRole().toUpperCase().trim() : "STUDENT";

        // Prohibit privilege escalation: prevent self-provisioning of ADMIN or RECRUITER after baseline seeding (CWE-269)
        if ("ADMIN".equals(rawRole) || "RECRUITER".equals(rawRole)) {
            if (userRepository.count() > 0) {
                log.warn("Unauthorized privilege escalation attempt: Public registration requested role {}", rawRole);
                throw new IllegalArgumentException("Privileged roles (ADMIN, RECRUITER) cannot be provisioned via public self-service registration.");
            }
        }

        // Use enterprise BCrypt key-stretched hashing (CWE-916 defense)
        String hashedPassword = passwordEncoder.encode(req.getPassword());

        User user = User.builder()
                .name(req.getName().trim())
                .email(req.getEmail().toLowerCase().trim())
                .passwordHash(hashedPassword)
                .role(rawRole)
                .department(req.getDepartment())
                .headline(req.getHeadline() != null ? req.getHeadline() : req.getDegree() + " Candidate")
                .bio(req.getBio())
                .college(req.getCollege())
                .degree(req.getDegree())
                .gradYear(req.getGradYear())
                .organizationName(req.getOrganizationName())
                .employeeId(req.getEmployeeId())
                .build();

        User saved = userRepository.save(user);
        log.info("Securely registered user: {} with role: {}", saved.getEmail(), saved.getRole());

        String token = generateSignedToken(saved);

        return AuthDto.AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(86400)
                .user(toDto(saved))
                .message("User registered successfully")
                .build();
    }

    public AuthDto.AuthResponse login(AuthDto.LoginRequest req) {
        if (req.getEmail() == null || req.getPassword() == null) {
            throw new IllegalArgumentException("Email and password are required.");
        }

        User user = userRepository.findByEmail(req.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        boolean authenticated = false;
        if (user.getPasswordHash() != null) {
            if (user.getPasswordHash().startsWith("$2a$") || user.getPasswordHash().startsWith("$2b$")) {
                // Enterprise BCrypt verification
                authenticated = passwordEncoder.matches(req.getPassword(), user.getPasswordHash());
            } else {
                // Backward-compatibility: Check legacy SHA-256 hash for existing seeded users
                String legacyHash = hashLegacySha256(req.getPassword());
                if (MessageDigest.isEqual(user.getPasswordHash().getBytes(StandardCharsets.UTF_8), legacyHash.getBytes(StandardCharsets.UTF_8))) {
                    authenticated = true;
                    // Transparently upgrade password hash to BCrypt (CWE-916 remediation)
                    user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
                    userRepository.save(user);
                    log.info("Automatically upgraded password hash to BCrypt (cost factor 12) for user: {}", user.getEmail());
                }
            }
        }

        if (!authenticated) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        String token = generateSignedToken(user);
        log.info("User authenticated securely: {} with role: {}", user.getEmail(), user.getRole());

        return AuthDto.AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(86400)
                .user(toDto(user))
                .message("Authentication successful")
                .build();
    }

    public AuthDto.UserDto getCurrentUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        return toDto(user);
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) return false;
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 2) return false;
            String payloadB64 = parts[0];
            String signatureB64 = parts[1];

            String expectedSig = signHmac(payloadB64);
            if (!MessageDigest.isEqual(
                signatureB64.getBytes(StandardCharsets.UTF_8),
                expectedSig.getBytes(StandardCharsets.UTF_8)
            )) {
                return false;
            }

            String payload = new String(Base64.getUrlDecoder().decode(payloadB64), StandardCharsets.UTF_8);
            String[] segments = payload.split(":");
            if (segments.length < 4) return false;

            long timestamp = Long.parseLong(segments[3]);
            long now = System.currentTimeMillis();
            // Check for expiration (24h) and prevent tokens from excessive future skew (>5min)
            if ((now - timestamp) > TOKEN_VALIDITY_MS || (timestamp - now) > 300_000L) {
                log.warn("Token expired or invalid timestamp: issuedAt={}, now={}", timestamp, now);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.debug("Token validation exception: {}", e.getMessage());
            return false;
        }
    }

    public TokenClaims parseToken(String token) {
        if (!validateToken(token)) {
            return null;
        }
        try {
            String payloadB64 = token.split("\\.")[0];
            String payload = new String(Base64.getUrlDecoder().decode(payloadB64), StandardCharsets.UTF_8);
            String[] segments = payload.split(":");
            return new TokenClaims(
                UUID.fromString(segments[0]),
                segments[1],
                segments[2].toUpperCase().trim(),
                Long.parseLong(segments[3])
            );
        } catch (Exception e) {
            log.warn("Failed to parse validated token claims: {}", e.getMessage());
            return null;
        }
    }

    private String hashLegacySha256(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((SALT + rawPassword).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }

    private String generateSignedToken(User user) {
        String payload = user.getId() + ":" + user.getEmail() + ":" + user.getRole() + ":" + System.currentTimeMillis();
        String payloadB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signature = signHmac(payloadB64);
        return payloadB64 + "." + signature;
    }

    private String signHmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC signature", e);
        }
    }

    private AuthDto.UserDto toDto(User user) {
        return AuthDto.UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .department(user.getDepartment())
                .headline(user.getHeadline())
                .bio(user.getBio())
                .college(user.getCollege())
                .degree(user.getDegree())
                .gradYear(user.getGradYear())
                .organizationName(user.getOrganizationName())
                .employeeId(user.getEmployeeId())
                .build();
    }
}
