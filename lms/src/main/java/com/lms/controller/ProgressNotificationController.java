package com.lms.controller;

import com.lms.dao.NotificationDAO;
import com.lms.service.ProgressService;
import com.lms.util.JsonUtil;
import com.lms.util.SessionUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * Progress and notification endpoints.
 *
 * GET  /api/progress/me          → student's progress across all courses
 * POST /api/progress/complete    → mark a lesson complete {lessonId, courseId}
 * GET  /api/notifications        → user's notifications
 * POST /api/notifications/read   → mark all read
 */
@WebServlet(urlPatterns = {"/api/progress/*", "/api/notifications/*"})
public class ProgressNotificationController extends HttpServlet {

    private final ProgressService    progressService    = new ProgressService();
    private final NotificationDAO    notificationDAO    = new NotificationDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = normPath(req);
        int userId = SessionUtil.getUserId(req);
        try {
            if (path.equals("/api/progress/me")) {
                JsonUtil.sendJson(resp, progressService.getStudentProgress(userId));
            } else if (path.equals("/api/progress/completed")) {
                String cIdStr = req.getParameter("courseId");
                if (cIdStr == null || cIdStr.isBlank()) {
                    JsonUtil.sendError(resp, 400, "courseId parameter required.");
                    return;
                }
                int courseId = Integer.parseInt(cIdStr);
                JsonUtil.sendJson(resp, progressService.getCompletedLessonIds(userId, courseId));
            } else if (path.equals("/api/notifications")) {
                JsonUtil.sendJson(resp, notificationDAO.findByUser(userId));
            } else {
                JsonUtil.sendError(resp, 404, "Not found.");
            }
        } catch (SQLException e) {
            JsonUtil.sendError(resp, 500, "Database error.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = normPath(req);
        int userId = SessionUtil.getUserId(req);
        try {
            if (path.equals("/api/progress/complete")) {
                Map<?, ?> body   = JsonUtil.parseBody(req, Map.class);
                int lessonId = ((Number) body.get("lessonId")).intValue();
                int courseId = ((Number) body.get("courseId")).intValue();
                var progress = progressService.completeLesson(userId, lessonId, courseId);
                JsonUtil.sendJson(resp, progress);
            } else if (path.equals("/api/notifications/read")) {
                notificationDAO.markAllRead(userId);
                JsonUtil.sendSuccess(resp, "All notifications marked as read.");
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

    private String normPath(HttpServletRequest req) {
        String uri = req.getRequestURI().substring(req.getContextPath().length());
        return uri.endsWith("/") && uri.length() > 1 ? uri.substring(0, uri.length() - 1) : uri;
    }
}
