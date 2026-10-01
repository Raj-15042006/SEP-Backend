package com.passport.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Production-Hardened HTTP Security Headers Filter
 * Enforces OWASP-recommended HTTP security headers to defend against XSS, clickjacking, MIME sniffing, and downgrade attacks.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Defend against Clickjacking
        response.setHeader("X-Frame-Options", "DENY");

        // 2. Prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");

        // 3. Strict Transport Security (HSTS) - 1 Year with subdomains & preload
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");

        // 4. Content Security Policy (CSP)
        response.setHeader("Content-Security-Policy", 
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                "font-src 'self' https://fonts.gstatic.com; " +
                "img-src 'self' data: https:; " +
                "connect-src 'self' https://*.supabase.co wss://*.supabase.co http://localhost:*; " +
                "frame-ancestors 'none'; " +
                "base-uri 'self'; " +
                "form-action 'self'");

        // 5. Referrer Policy
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // 6. Permissions Policy
        response.setHeader("Permissions-Policy", "geolocation=(), camera=(), microphone=(), payment=(), usb=()");

        // 7. Cross-Origin Embedder and Opener Policies
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        response.setHeader("Cross-Origin-Resource-Policy", "cross-origin");

        filterChain.doFilter(request, response);
    }
}
