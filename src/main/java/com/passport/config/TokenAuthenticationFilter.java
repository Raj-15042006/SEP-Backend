package com.passport.config;

import com.passport.auth.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            AuthService.TokenClaims claims = authService.parseToken(token);
            if (claims != null) {
                try {
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + claims.getRole());
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(claims.getUserId(), null, Collections.singletonList(authority));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authenticated user {} with role {}", claims.getEmail(), claims.getRole());
                } catch (Exception e) {
                    log.warn("Failed to set security context for authenticated token: {}", e.getMessage());
                }
            } else {
                log.debug("Token invalid, expired, or malformed for request {}", request.getRequestURI());
            }
        }

        filterChain.doFilter(request, response);
    }
}
