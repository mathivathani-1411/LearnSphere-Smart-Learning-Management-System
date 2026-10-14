package com.lms.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.Set;

/**
 * Session management helpers.
 * Session attribute keys are centralised here to avoid typos.
 */
public class SessionUtil {

    public static final String ATTR_USER_ID   = "userId";
    public static final String ATTR_USER_NAME = "userName";
    public static final String ATTR_USER_ROLE = "userRole";
    public static final String ATTR_CSRF      = "csrfToken";

    private static final int SESSION_TIMEOUT_SECONDS = 30 * 60; // 30 minutes

    private SessionUtil() {}

    /** Create a new authenticated session for the given user. */
    public static void createSession(HttpServletRequest req, int userId, String name, String role) {
        // Invalidate any existing session first (session fixation prevention)
        HttpSession old = req.getSession(false);
        if (old != null) old.invalidate();

        HttpSession session = req.getSession(true);
        session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);
        session.setAttribute(ATTR_USER_ID,   userId);
        session.setAttribute(ATTR_USER_NAME, name);
        session.setAttribute(ATTR_USER_ROLE, role);
        session.setAttribute(ATTR_CSRF,      generateCsrfToken());
    }

    /** Invalidate the session on logout. */
    public static void invalidate(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
    }

    /** Return the logged-in user's ID, or -1 if not authenticated. */
    public static int getUserId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return -1;
        Object id = session.getAttribute(ATTR_USER_ID);
        return (id instanceof Integer) ? (Integer) id : -1;
    }

    /** Return the logged-in user's role, or null if not authenticated. */
    public static String getRole(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        Object role = session.getAttribute(ATTR_USER_ROLE);
        return (role instanceof String) ? (String) role : null;
    }

    /** Return the logged-in user's name, or null if not authenticated. */
    public static String getUserName(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        Object name = session.getAttribute(ATTR_USER_NAME);
        return (name instanceof String) ? (String) name : null;
    }

    /** True if the user has a valid session. */
    public static boolean isAuthenticated(HttpServletRequest req) {
        return getUserId(req) > 0;
    }

    /** True if the authenticated user holds the given role. */
    public static boolean hasRole(HttpServletRequest req, String... roles) {
        String userRole = getRole(req);
        if (userRole == null) return false;
        return Set.of(roles).contains(userRole);
    }

    /** Validate a CSRF token against the one stored in the session. */
    public static boolean isValidCsrf(HttpServletRequest req, String token) {
        HttpSession session = req.getSession(false);
        if (session == null || token == null) return false;
        String stored = (String) session.getAttribute(ATTR_CSRF);
        return token.equals(stored);
    }

    private static String generateCsrfToken() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
