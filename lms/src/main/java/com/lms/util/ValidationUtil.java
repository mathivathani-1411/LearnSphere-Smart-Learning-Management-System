package com.lms.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

/**
 * Input validation and sanitisation utilities.
 */
public class ValidationUtil {

    private static final int MAX_EMAIL_LEN   = 255;
    private static final int MAX_NAME_LEN    = 150;
    private static final int MIN_PASSWORD_LEN = 6;
    private static final long MAX_FILE_BYTES  = 10L * 1024 * 1024; // 10 MB

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "doc", "zip", "txt");

    private ValidationUtil() {}

    public static boolean isValidEmail(String email) {
        return email != null && email.length() <= MAX_EMAIL_LEN
                && email.matches("^[\\w.+\\-]+@(?:[a-zA-Z0-9\\-]+\\.)+[a-zA-Z]{2,}$");
    }

    public static boolean isValidName(String name) {
        return name != null && !name.isBlank() && name.length() <= MAX_NAME_LEN;
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= MIN_PASSWORD_LEN;
    }

    public static boolean isValidFileSize(long sizeBytes) {
        return sizeBytes > 0 && sizeBytes <= MAX_FILE_BYTES;
    }

    public static boolean isAllowedExtension(String filename) {
        if (filename == null) return false;
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return false;
        String ext = filename.substring(dot + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(ext);
    }

    /**
     * Basic XSS mitigation — HTML-encode special characters before echoing
     * user content into HTML (prefer Thymeleaf/JSP EL in production).
     */
    public static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }

    /** Strip any null characters, script tags, and trim whitespace. */
    public static String sanitise(String input) {
        if (input == null) return null;
        String cleaned = input.replace("\0", "");
        cleaned = cleaned.replaceAll("(?is)<script.*?>.*?</script>", "");
        cleaned = cleaned.replaceAll("(?i)</?script.*?>", "");
        return cleaned.trim();
    }

    /** Parse an int path parameter; return -1 on failure. */
    public static int parseId(String s) {
        try {
            int id = Integer.parseInt(s);
            return id > 0 ? id : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Extract the last path segment as an integer ID.
     * e.g. /api/courses/42 → 42
     */
    public static int extractIdFromPath(HttpServletRequest req) {
        String[] parts = req.getPathInfo() == null ? new String[0]
                : req.getPathInfo().split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            int id = parseId(parts[i]);
            if (id > 0) return id;
        }
        return -1;
    }
}
