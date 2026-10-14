package com.lms.controller;

import com.lms.model.User;
import com.lms.service.AuthService;
import com.lms.util.JsonUtil;
import com.lms.util.SessionUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * Handles authentication endpoints:
 *   POST /api/auth/register
 *   POST /api/auth/login
 *   POST /api/auth/logout
 */
@WebServlet(urlPatterns = {"/api/auth/register", "/api/auth/login", "/api/auth/logout", "/api/auth/me"})
public class AuthController extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getServletPath();
        if ("/api/auth/me".equals(path)) {
            if (!SessionUtil.isAuthenticated(req)) {
                JsonUtil.sendError(resp, 401, "Not logged in.");
                return;
            }
            JsonUtil.sendJson(resp, Map.of(
                    "authenticated", true,
                    "userId", SessionUtil.getUserId(req),
                    "name", SessionUtil.getUserName(req) != null ? SessionUtil.getUserName(req) : "User",
                    "role", SessionUtil.getRole(req) != null ? SessionUtil.getRole(req) : "STUDENT"
            ));
        } else {
            JsonUtil.sendError(resp, 404, "Not found.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getServletPath();
        switch (path) {
            case "/api/auth/register" -> handleRegister(req, resp);
            case "/api/auth/login"    -> handleLogin(req, resp);
            case "/api/auth/logout"   -> handleLogout(req, resp);
            default -> JsonUtil.sendError(resp, 404, "Not found.");
        }
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Map<?, ?> body = JsonUtil.parseBody(req, Map.class);
            String name     = getString(body, "name");
            String email    = getString(body, "email");
            String password = getString(body, "password");
            String role     = getString(body, "role");

            User user = authService.register(name, email, password, role);
            // Auto-login after registration
            SessionUtil.createSession(req, user.getUserId(), user.getName(), user.getRole());
            JsonUtil.sendJson(resp, HttpServletResponse.SC_CREATED,
                    Map.of("message", "Registration successful.", "role", user.getRole(), "name", user.getName()));
        } catch (IllegalArgumentException e) {
            JsonUtil.sendError(resp, 400, e.getMessage());
        } catch (SQLException e) {
            JsonUtil.sendError(resp, 500, "Registration failed. Please try again.");
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Map<?, ?> body  = JsonUtil.parseBody(req, Map.class);
            String email    = getString(body, "email");
            String password = getString(body, "password");

            User user = authService.login(email, password);
            SessionUtil.createSession(req, user.getUserId(), user.getName(), user.getRole());

            JsonUtil.sendJson(resp, Map.of(
                    "message", "Login successful.",
                    "role",  user.getRole(),
                    "name",  user.getName(),
                    "userId", user.getUserId()
            ));
        } catch (IllegalArgumentException e) {
            JsonUtil.sendError(resp, 401, e.getMessage());
        } catch (SQLException e) {
            JsonUtil.sendError(resp, 500, "Login failed.");
        }
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        SessionUtil.invalidate(req);
        JsonUtil.sendSuccess(resp, "Logged out successfully.");
    }

    private String getString(Map<?, ?> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString().trim() : null;
    }
}
