package com.lms.controller;

import com.lms.model.Course;
import com.lms.service.AssignmentService;
import com.lms.service.ContentService;
import com.lms.service.CourseService;
import com.lms.util.JsonUtil;
import com.lms.util.SessionUtil;
import com.lms.util.ValidationUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Course CRUD and enrollment.
 *
 * GET    /api/courses             → list published courses
 * GET    /api/courses/{id}        → get single course
 * POST   /api/courses             → create course (INSTRUCTOR)
 * PUT    /api/courses/{id}        → update course (INSTRUCTOR, owner)
 * DELETE /api/courses/{id}        → delete course (INSTRUCTOR, owner)
 * POST   /api/courses/{id}/enroll → enroll (STUDENT)
 * GET    /api/enrollments/me      → my enrollments (STUDENT)
 */
@WebServlet(urlPatterns = {"/api/courses/*", "/api/enrollments/*"})
public class CourseController extends HttpServlet {

    private final CourseService   courseService   = new CourseService();
    private final ContentService  contentService  = new ContentService();
    private final AssignmentService assignmentService = new AssignmentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = normPath(req);
        try {
            if (path.equals("/api/courses") || path.equals("/api/courses/")) {
                List<Course> courses = courseService.getPublishedCourses();
                JsonUtil.sendJson(resp, courses);

            } else if (path.equals("/api/courses/instructor")) {
                requireRole(req, resp, "INSTRUCTOR");
                int instructorId = SessionUtil.getUserId(req);
                JsonUtil.sendJson(resp, courseService.getInstructorCourses(instructorId));

            } else if (path.matches("/api/courses/\\d+/modules")) {
                int courseId = extractCourseId(path);
                JsonUtil.sendJson(resp, contentService.getModules(courseId));

            } else if (path.matches("/api/courses/\\d+/assignments")) {
                int courseId = extractCourseId(path);
                JsonUtil.sendJson(resp, assignmentService.getAssignmentsByCourse(courseId));

            } else if (path.matches("/api/courses/\\d+/lessons")) {
                int courseId = extractCourseId(path);
                int userId = SessionUtil.getUserId(req);
                String role = SessionUtil.getRole(req);
                JsonUtil.sendJson(resp, contentService.getLessonsForCourse(courseId, userId, role));

            } else if (path.matches("/api/courses/\\d+")) {
                int id = ValidationUtil.extractIdFromPath(req);
                int userId = SessionUtil.getUserId(req);
                String role  = SessionUtil.getRole(req);
                Optional<Course> c = courseService.getCourse(id, userId, role);
                if (c.isPresent()) {
                    JsonUtil.sendJson(resp, c.get());
                } else {
                    JsonUtil.sendError(resp, 404, "Course not found.");
                }

            } else if (path.equals("/api/enrollments/me")) {
                int studentId = SessionUtil.getUserId(req);
                JsonUtil.sendJson(resp, new com.lms.dao.EnrollmentDAO().findByStudent(studentId));

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
        try {
            if (path.equals("/api/courses") || path.equals("/api/courses/")) {
                // Create course — INSTRUCTOR only
                requireRole(req, resp, "INSTRUCTOR");
                Map<?, ?> body = JsonUtil.parseBody(req, Map.class);
                String title = str(body, "title");
                String desc  = str(body, "description");
                Course course = courseService.createCourse(title, desc, SessionUtil.getUserId(req));
                JsonUtil.sendJson(resp, HttpServletResponse.SC_CREATED, course);

            } else if (path.matches("/api/courses/\\d+/modules")) {
                // Add module — INSTRUCTOR only
                requireRole(req, resp, "INSTRUCTOR");
                int courseId = extractCourseId(path);
                Map<?, ?> body = JsonUtil.parseBody(req, Map.class);
                String title = str(body, "title");
                int order = body.get("displayOrder") != null ? ((Number) body.get("displayOrder")).intValue() : 1;
                var mod = contentService.createModule(courseId, title, order, SessionUtil.getUserId(req));
                JsonUtil.sendJson(resp, HttpServletResponse.SC_CREATED, mod);

            } else if (path.matches("/api/courses/\\d+/assignments")) {
                // Create assignment — INSTRUCTOR only
                requireRole(req, resp, "INSTRUCTOR");
                int courseId = extractCourseId(path);
                Map<?, ?> body = JsonUtil.parseBody(req, Map.class);
                String title   = str(body, "title");
                String desc    = str(body, "description");
                String dlStr   = str(body, "deadline");
                int maxMarks   = body.get("maxMarks") != null ? ((Number) body.get("maxMarks")).intValue() : 100;
                boolean late   = body.get("allowLate") instanceof Boolean ? (Boolean) body.get("allowLate") : false;
                LocalDateTime deadline;
                try { deadline = LocalDateTime.parse(dlStr); }
                catch (DateTimeParseException e) { JsonUtil.sendError(resp, 400, "Invalid deadline format. Use ISO-8601."); return; }
                var a = assignmentService.createAssignment(courseId, title, desc, deadline, maxMarks, late, SessionUtil.getUserId(req));
                JsonUtil.sendJson(resp, HttpServletResponse.SC_CREATED, a);

            } else if (path.matches("/api/courses/\\d+/enroll")) {
                // Enroll — STUDENT only
                requireRole(req, resp, "STUDENT");
                int courseId  = extractCourseIdFromEnroll(req);
                int studentId = SessionUtil.getUserId(req);
                courseService.enroll(studentId, courseId);
                JsonUtil.sendSuccess(resp, "Enrolled successfully.");

            } else {
                JsonUtil.sendError(resp, 404, "Not found.");
            }
        } catch (IllegalArgumentException e) {
            JsonUtil.sendError(resp, 400, e.getMessage());
        } catch (IllegalStateException e) {
            JsonUtil.sendError(resp, 409, e.getMessage());
        } catch (SecurityException e) {
            JsonUtil.sendError(resp, 403, e.getMessage());
        } catch (SQLException e) {
            JsonUtil.sendError(resp, 500, "Database error.");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = normPath(req);
        try {
            if (path.matches("/api/courses/\\d+")) {
                requireRole(req, resp, "INSTRUCTOR");
                int id = ValidationUtil.extractIdFromPath(req);
                Map<?, ?> body = JsonUtil.parseBody(req, Map.class);
                Course course = courseService.updateCourse(id, str(body, "title"),
                        str(body, "description"), str(body, "status"), SessionUtil.getUserId(req));
                JsonUtil.sendJson(resp, course);
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
            if (path.matches("/api/courses/\\d+")) {
                requireRole(req, resp, "INSTRUCTOR");
                int id = ValidationUtil.extractIdFromPath(req);
                courseService.deleteCourse(id, SessionUtil.getUserId(req));
                JsonUtil.sendSuccess(resp, "Course deleted.");
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

    // ---- helpers ----

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

    private int extractCourseId(String path) {
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("courses".equals(parts[i])) {
                return Integer.parseInt(parts[i + 1]);
            }
        }
        throw new IllegalArgumentException("Invalid course ID in path.");
    }

    private int extractCourseIdFromEnroll(HttpServletRequest req) {
        // Path pattern: /api/courses/{id}/enroll
        String[] parts = req.getRequestURI().split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("courses".equals(parts[i])) {
                try { return Integer.parseInt(parts[i + 1]); } catch (NumberFormatException e) { break; }
            }
        }
        throw new IllegalArgumentException("Invalid course ID in path.");
    }
}
