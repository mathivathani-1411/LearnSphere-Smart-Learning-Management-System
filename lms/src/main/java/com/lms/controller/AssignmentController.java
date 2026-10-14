package com.lms.controller;

import com.lms.service.AssignmentService;
import com.lms.service.FileStorageService;
import com.lms.util.JsonUtil;
import com.lms.util.SessionUtil;
import com.lms.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * Handles assignment and submission endpoints.
 *
 * POST   /api/courses/{id}/assignments       → create assignment (INSTRUCTOR)
 * GET    /api/assignments/{id}               → get assignment
 * GET    /api/assignments/me                 → student's assignments
 * POST   /api/assignments/{id}/submissions   → submit (multipart, STUDENT)
 * GET    /api/assignments/{id}/submissions   → list submissions (INSTRUCTOR)
 * PUT    /api/submissions/{id}/grade         → grade submission (INSTRUCTOR)
 */
@WebServlet(urlPatterns = {"/api/assignments/*", "/api/submissions/*"})
@MultipartConfig(maxFileSize = 10 * 1024 * 1024, maxRequestSize = 11 * 1024 * 1024)
public class AssignmentController extends HttpServlet {

    private final AssignmentService  assignmentService  = new AssignmentService();
    private final FileStorageService fileStorageService = new FileStorageService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = normPath(req);
        try {
            if (path.equals("/api/assignments/me")) {
                requireRole(req, resp, "STUDENT");
                JsonUtil.sendJson(resp, assignmentService.getAssignmentsForStudent(SessionUtil.getUserId(req)));
            } else if (path.equals("/api/submissions/me")) {
                requireRole(req, resp, "STUDENT");
                JsonUtil.sendJson(resp, assignmentService.getMySubmissions(SessionUtil.getUserId(req)));
            } else if (path.matches("/api/assignments/\\d+")) {
                int id = ValidationUtil.extractIdFromPath(req);
                assignmentService.getAssignment(id)
                        .ifPresentOrElse(
                                a -> { try { JsonUtil.sendJson(resp, a); } catch (IOException e) { throw new RuntimeException(e); } },
                                () -> { try { JsonUtil.sendError(resp, 404, "Assignment not found."); } catch (IOException e) { throw new RuntimeException(e); } }
                        );
            } else if (path.matches("/api/assignments/\\d+/submissions")) {
                requireRole(req, resp, "INSTRUCTOR");
                int asgId = extractFirstNumericSegment(req, "assignments");
                JsonUtil.sendJson(resp, assignmentService.getSubmissionsForAssignment(asgId, SessionUtil.getUserId(req)));
            } else if (path.matches("/api/submissions/\\d+/download")) {
                int subId = extractFirstNumericSegment(req, "submissions");
                int userId = SessionUtil.getUserId(req);
                String role = SessionUtil.getRole(req);
                var sub = assignmentService.getSubmission(subId, userId, role);

                java.nio.file.Path filePath = fileStorageService.resolve(sub.getFilePath());
                if (!java.nio.file.Files.exists(filePath)) {
                    JsonUtil.sendError(resp, 404, "File not found on server.");
                    return;
                }

                String mimeType = getServletContext().getMimeType(filePath.getFileName().toString());
                if (mimeType == null) mimeType = "application/octet-stream";
                resp.setContentType(mimeType);
                resp.setContentLengthLong(java.nio.file.Files.size(filePath));
                String fileName = sub.getOriginalName() != null ? sub.getOriginalName() : filePath.getFileName().toString();
                resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName.replace("\"", "") + "\"");

                try (java.io.InputStream in = java.nio.file.Files.newInputStream(filePath);
                     java.io.OutputStream out = resp.getOutputStream()) {
                    in.transferTo(out);
                }
                return;
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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        String path = normPath(req);
        try {
            if (path.matches("/api/assignments/\\d+/submissions")) {
                // Submit assignment
                requireRole(req, resp, "STUDENT");
                int assignmentId = extractFirstNumericSegment(req, "assignments");
                int studentId    = SessionUtil.getUserId(req);

                Part filePart = req.getPart("file");
                if (filePart == null) { JsonUtil.sendError(resp, 400, "No file uploaded."); return; }

                String originalName = getFileName(filePart);
                if (!ValidationUtil.isAllowedExtension(originalName)) {
                    JsonUtil.sendError(resp, 400, "Invalid file type. Allowed: pdf, docx, doc, zip, txt."); return;
                }
                if (!ValidationUtil.isValidFileSize(filePart.getSize())) {
                    JsonUtil.sendError(resp, 400, "File must be 1 byte – 10 MB."); return;
                }

                String relPath = fileStorageService.save(filePart, "submissions");
                var sub = assignmentService.submit(assignmentId, studentId, relPath, originalName);
                JsonUtil.sendJson(resp, HttpServletResponse.SC_CREATED, sub);

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
            if (path.matches("/api/submissions/\\d+/grade")) {
                requireRole(req, resp, "INSTRUCTOR");
                int submissionId = extractFirstNumericSegment(req, "submissions");
                Map<?, ?> body = JsonUtil.parseBody(req, Map.class);
                double score   = body.get("score") != null ? ((Number) body.get("score")).doubleValue() : -1;
                String feedback = str(body, "feedback");
                var grade = assignmentService.grade(submissionId, score, feedback, SessionUtil.getUserId(req));
                JsonUtil.sendJson(resp, grade);
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

    private int extractFirstNumericSegment(HttpServletRequest req, String preceding) {
        String[] parts = req.getRequestURI().split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if (preceding.equals(parts[i])) {
                try { return Integer.parseInt(parts[i + 1]); } catch (NumberFormatException e) { break; }
            }
        }
        throw new IllegalArgumentException("Invalid ID in path.");
    }

    private String getFileName(Part part) {
        String cd = part.getHeader("content-disposition");
        if (cd != null) {
            for (String token : cd.split(";")) {
                token = token.trim();
                if (token.startsWith("filename")) {
                    return token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
                }
            }
        }
        return "upload.bin";
    }
}
