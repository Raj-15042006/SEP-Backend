package com.passport.common;

import org.springframework.stereotype.Component;

/**
 * PII Masking & Privacy Guard Engine
 * Automatically redacts candidate personal identifiable information (emails, roll numbers, phone numbers)
 * for unverified public queries and recruiter search interfaces.
 */
@Component
public class PiiDataMasker {

    /**
     * Masks an email address: e.g. "alex.rivera@university.edu" -> "a***a@u***.edu"
     */
    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String user = parts[0];
        String domain = parts[1];

        String maskedUser = user.length() <= 2 
                ? user.charAt(0) + "***" 
                : user.charAt(0) + "***" + user.charAt(user.length() - 1);

        String[] domainParts = domain.split("\\.");
        String maskedDomain = domainParts[0].charAt(0) + "***." + (domainParts.length > 1 ? domainParts[domainParts.length - 1] : "com");

        return maskedUser + "@" + maskedDomain;
    }

    /**
     * Masks a student/faculty identifier: e.g. "2022CS014" -> "2022***14"
     */
    public String maskIdentifier(String id) {
        if (id == null || id.length() <= 4) return id;
        return id.substring(0, 3) + "***" + id.substring(id.length() - 2);
    }

    /**
     * Masks a phone number: e.g. "+919876543210" -> "+91******3210"
     */
    public String maskPhone(String phone) {
        if (phone == null || phone.length() <= 6) return phone;
        return phone.substring(0, 3) + "******" + phone.substring(phone.length() - 4);
    }
}
