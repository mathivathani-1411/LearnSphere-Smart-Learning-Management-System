package com.lms.filter;

import com.lms.util.SessionUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

/**
 * Authentication filter — protects all /api/* and page-specific paths.
 * Public paths (login, register, published catalogue) are whitelisted.
 */
@WebFilter(urlPatterns = {"/api/*", "/student/*", "/instructor/*"})
public class AuthFilter implements Filter {

    /** Paths that can be accessed without a session. */
    private static final Set<String> PUBLIC_API_PATHS = Set.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/courses"          // GET catalogue is public
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // Security headers on every response
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("X-XSS-Protection", "1; mode=block");
        resp.setHeader("Referrer-Policy", "same-origin");

        String path   = req.getRequestURI().substring(req.getContextPath().length());
        String method = req.getMethod();

        // Allow public endpoints
        if (isPublic(path, method)) {
            chain.doFilter(request, response);
            return;
        }

        // Require a valid session
        if (!SessionUtil.isAuthenticated(req)) {
            if (isApiPath(path)) {
                resp.setContentType("application/json");
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write("{\"error\":\"Authentication required.\",\"status\":401}");
            } else {
                resp.sendRedirect(req.getContextPath() + "/login.html");
            }
            return;
        }

        // Role-based path enforcement
        String role = SessionUtil.getRole(req);
        if (path.startsWith("/instructor/") && !"INSTRUCTOR".equals(role)) {
            resp.sendRedirect(req.getContextPath() + "/student/dashboard.html");
            return;
        }
        if (path.startsWith("/student/") && !"STUDENT".equals(role)) {
            resp.sendRedirect(req.getContextPath() + "/instructor/dashboard.html");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isPublic(String path, String method) {
        if (PUBLIC_API_PATHS.contains(path)) return true;
        // GET /api/courses/{id} is public
        if ("GET".equals(method) && path.matches("/api/courses/\\d+")) return true;
        return false;
    }

    private boolean isApiPath(String path) {
        return path.startsWith("/api/");
    }
}
