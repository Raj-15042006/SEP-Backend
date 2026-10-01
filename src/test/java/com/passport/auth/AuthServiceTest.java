package com.passport.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .name("Aarav Sharma")
                .email("aarav.sharma@university.edu")
                .passwordHash(passwordEncoder.encode("password123"))
                .role("STUDENT")
                .college("National Institute of Technology")
                .degree("B.Tech CSE")
                .gradYear(2026)
                .build();
    }

    @Test
    @DisplayName("Should successfully register new user with valid details and signed token")
    void testRegisterSuccess() {
        AuthDto.RegisterRequest request = AuthDto.RegisterRequest.builder()
                .name("Aarav Sharma")
                .email("aarav.sharma@university.edu")
                .password("securePassword123")
                .role("STUDENT")
                .college("National Institute of Technology")
                .degree("B.Tech CSE")
                .gradYear(2026)
                .build();

        when(userRepository.existsByEmail("aarav.sharma@university.edu")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        AuthDto.AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertTrue(response.getToken().contains(".")); // HMAC signed format
        assertEquals("Aarav Sharma", response.getUser().getName());
        assertEquals("STUDENT", response.getUser().getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject registration when email already exists")
    void testRegisterDuplicateEmail() {
        AuthDto.RegisterRequest request = AuthDto.RegisterRequest.builder()
                .name("Aarav Sharma")
                .email("aarav.sharma@university.edu")
                .password("password123")
                .role("STUDENT")
                .build();

        when(userRepository.existsByEmail("aarav.sharma@university.edu")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject registration when password is too short")
    void testRegisterShortPassword() {
        AuthDto.RegisterRequest request = AuthDto.RegisterRequest.builder()
                .name("Aarav Sharma")
                .email("aarav.sharma@university.edu")
                .password("123")
                .role("STUDENT")
                .build();

        when(userRepository.existsByEmail("aarav.sharma@university.edu")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    @DisplayName("Should successfully login user with correct credentials and validate token signature")
    void testLoginSuccess() {
        AuthDto.LoginRequest loginReq = AuthDto.LoginRequest.builder()
                .email("aarav.sharma@university.edu")
                .password("password123")
                .build();

        when(userRepository.findByEmail("aarav.sharma@university.edu")).thenReturn(Optional.of(sampleUser));

        AuthDto.AuthResponse response = authService.login(loginReq);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertTrue(authService.validateToken(response.getToken()));
    }

    @Test
    @DisplayName("Should reject login when user not found")
    void testLoginUserNotFound() {
        AuthDto.LoginRequest loginReq = AuthDto.LoginRequest.builder()
                .email("unknown@university.edu")
                .password("password123")
                .build();

        when(userRepository.findByEmail("unknown@university.edu")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.login(loginReq));
    }
}
