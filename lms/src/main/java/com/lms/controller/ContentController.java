package com.lms.controller;

import com.lms.model.Lesson;
import com.lms.service.ContentService;
import com.lms.util.JsonUtil;
import com.lms.util.SessionUtil;
import com.lms.util.ValidationUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

/**
 * Handles module & lesson specific operations:
 *   POST   /api/modules/{id}/lessons  → create lesson in module (INSTRUCTOR)
 *   GET    /api/lessons/{id}          → get lesson details
 *   DELETE /api/lessons/{id}          → delete lesson (INSTRUCTOR)
 *   DELETE /api/modules/{id}          → delete module (INSTRUCTOR)
 */
@WebServlet(urlPatterns = {"/api/modules/*", "/api/lessons/*"})
public class ContentController extends HttpServlet {

    private final ContentService contentService = new ContentService();
    private final com.lms.dao.ModuleLessonDAO lessonDAO = new com.lms.dao.ModuleLessonDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = normPath(req);
        try {
            if (path.matches("/api/lessons/\\d+")) {
                int lessonId = ValidationUtil.extractIdFromPath(req);
                int userId = SessionUtil.getUserId(req);
                String role = SessionUtil.getRole(req);
                Optional<Lesson> lesson = contentService.getLesson(lessonId, userId, role);
                if (lesson.isPresent()) {
                    JsonUtil.sendJson(resp, lesson.get());
                } else {
                    JsonUtil.sendError(resp, 404, "Lesson not found.");
                }
            } else {
                JsonUtil.sendError(resp, 404, "Not found.");
            }
        } catch (SecurityException e) {
            JsonUtil.sendError(resp, 403, e.getMessage());
        } catch (SQLException e) {
            JsonUtil.sendError(resp, 500, "Database error.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = normPath(req);
        try {
            if (path.matches("/api/modules/\\d+/lessons")) {
                requireRole(req, resp, "INSTRUCTOR");
                int moduleId = extractModuleId(path);
                Map<?, ?> body = JsonUtil.parseBody(req, Map.class);
                String title = str(body, "title");
                String content = str(body, "content");
                String contentType = str(body, "contentType");
                int displayOrder = body.get("displayOrder") != null ? ((Number) body.get("displayOrder")).intValue() : 1;

                Lesson lesson = contentService.createLesson(moduleId, title, content, contentType, displayOrder, SessionUtil.getUserId(req));
                JsonUtil.sendJson(resp, HttpServletResponse.SC_CREATED, lesson);
            } else {
                JsonUtil.sendError(resp, 404, "Not found.");
            }
        } catch (IllegalArgumentException e) {
            JsonUtil.sendError(resp, 400, e.getMessage());
        } catch (SecurityException e) {
            JsonUtil.sendError(resp, 403, e.getMessage());
        } catch (SQLException e) {
            JsonUtil.sendError(resp, 500, "Database error.");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = normPath(req);
        try {
            requireRole(req, resp, "INSTRUCTOR");
            if (path.matches("/api/lessons/\\d+")) {
                int id = ValidationUtil.extractIdFromPath(req);
                lessonDAO.deleteLesson(id);
                JsonUtil.sendSuccess(resp, "Lesson deleted.");
            } else if (path.matches("/api/modules/\\d+")) {
                int id = ValidationUtil.extractIdFromPath(req);
                lessonDAO.deleteModule(id);
                JsonUtil.sendSuccess(resp, "Module deleted.");
            } else {
                JsonUtil.sendError(resp, 404, "Not found.");
            }
        } catch (SQLException e) {
            JsonUtil.sendError(resp, 500, "Database error.");
        }
    }

    private String normPath(HttpServletRequest req) {
        String uri = req.getRequestURI().substring(req.getContextPath().length());
        return uri.endsWith("/") && uri.length() > 1 ? uri.substring(0, uri.length() - 1) : uri;
    }

    private String str(Map<?, ?> m, String k) {
        Object v = m.get(k); return v != null ? v.toString().trim() : null;
    }

    private void requireRole(HttpServletRequest req, HttpServletResponse resp, String role) throws IOException {
        if (!SessionUtil.hasRole(req, role)) {
            JsonUtil.sendError(resp, 403, "Access denied.");
            throw new SecurityException("Role mismatch");
        }
    }

    private int extractModuleId(String path) {
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("modules".equals(parts[i])) {
                return Integer.parseInt(parts[i + 1]);
            }
        }
        throw new IllegalArgumentException("Invalid module ID in path.");
    }
}
