package com.lms.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ValidationUtil Unit Tests")
class ValidationUtilTest {

    @ParameterizedTest
    @ValueSource(strings = {"user@example.com", "alice.johnson@domain.co.in", "student_123@university.edu"})
    @DisplayName("Valid email addresses should return true")
    void testValidEmails(String email) {
        assertTrue(ValidationUtil.isValidEmail(email));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-email", "user@", "@domain.com", "user@domain", "", "   "})
    @DisplayName("Invalid email addresses should return false")
    void testInvalidEmails(String email) {
        assertFalse(ValidationUtil.isValidEmail(email));
    }

    @Test
    @DisplayName("Password length validation (>= 6 characters)")
    void testPasswordValidation() {
        assertTrue(ValidationUtil.isValidPassword("secret123"));
        assertTrue(ValidationUtil.isValidPassword("123456"));
        assertFalse(ValidationUtil.isValidPassword("12345"));
        assertFalse(ValidationUtil.isValidPassword(""));
        assertFalse(ValidationUtil.isValidPassword(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"submission.pdf", "project.docx", "report.doc", "archive.zip", "notes.txt"})
    @DisplayName("Allowed document upload extensions")
    void testAllowedExtensions(String filename) {
        assertTrue(ValidationUtil.isAllowedExtension(filename));
    }

    @ParameterizedTest
    @ValueSource(strings = {"script.exe", "virus.sh", "shell.bat", "hack.php", "page.html", "data.js"})
    @DisplayName("Blocked dangerous upload extensions")
    void testBlockedExtensions(String filename) {
        assertFalse(ValidationUtil.isAllowedExtension(filename));
    }

    @Test
    @DisplayName("HTML sanitization strips dangerous script tags")
    void testSanitise() {
        String unsafe = "<script>alert('xss')</script>Hello World";
        String clean = ValidationUtil.sanitise(unsafe);
        assertFalse(clean.contains("<script>"));
        assertTrue(clean.contains("Hello World"));
    }
}
