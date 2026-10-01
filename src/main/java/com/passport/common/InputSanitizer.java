package com.passport.common;

import java.util.regex.Pattern;

/**
 * Enterprise Web Security Input Sanitizer
 * Defends against Cross-Site Scripting (XSS), HTML injection, and URL pseudo-protocol execution (CWE-79).
 */
public final class InputSanitizer {

    private InputSanitizer() {}

    // Regex to match and strip HTML tags
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>", Pattern.CASE_INSENSITIVE);

    // Regex to strip dangerous script pseudo-protocols
    private static final Pattern SCRIPT_PROTOCOL_PATTERN = Pattern.compile("javascript:|vbscript:|data:text/html", Pattern.CASE_INSENSITIVE);

    // Regex to strip dangerous inline JS event handlers (e.g. onerror=, onload=, onclick=)
    private static final Pattern EVENT_HANDLER_PATTERN = Pattern.compile("(?i)on[a-z]+\\s*=", Pattern.CASE_INSENSITIVE);

    /**
     * Sanitizes user-provided text inputs (e.g., titles, descriptions, rubric comments).
     * Strips dangerous HTML tags, inline event handlers, and decodes harmless whitespace.
     */
    public static String sanitizeText(String input) {
        if (input == null) {
            return null;
        }

        String cleaned = input.trim();
        if (cleaned.isEmpty()) {
            return "";
        }

        // 1. Remove dangerous script protocols
        cleaned = SCRIPT_PROTOCOL_PATTERN.matcher(cleaned).replaceAll("");

        // 2. Remove inline event handlers
        cleaned = EVENT_HANDLER_PATTERN.matcher(cleaned).replaceAll("");

        // 3. Strip all HTML/XML tags
        cleaned = HTML_TAG_PATTERN.matcher(cleaned).replaceAll("");

        return cleaned.trim();
    }

    /**
     * Validates and sanitizes hyperlinks (e.g. GitHub repoUrl, live demo URL).
     * Strictly limits protocols to https://, http://, or relative paths.
     */
    public static String sanitizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        String trimmed = url.trim();
        // Disallow dangerous URI schemes
        if (SCRIPT_PROTOCOL_PATTERN.matcher(trimmed).find()) {
            throw new IllegalArgumentException("Invalid URL: Untrusted pseudo-protocol detected for security compliance.");
        }

        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://") && !trimmed.startsWith("/")) {
            throw new IllegalArgumentException("Invalid URL: Protocol must be http:// or https://");
        }

        // Strip HTML injection from URLs
        return HTML_TAG_PATTERN.matcher(trimmed).replaceAll("");
    }
}
