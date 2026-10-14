package com.lms.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared helper for writing JSON responses from servlet controllers.
 */
public class JsonUtil {

    private static final Logger log = LoggerFactory.getLogger(JsonUtil.class);
    private static final ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper();
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private JsonUtil() {}

    /** Write an object as a JSON response (200 OK). */
    public static void sendJson(HttpServletResponse resp, Object data) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        MAPPER.writeValue(resp.getWriter(), data);
    }

    /** Write a JSON response with a custom HTTP status code. */
    public static void sendJson(HttpServletResponse resp, int status, Object data) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");
        resp.setStatus(status);
        MAPPER.writeValue(resp.getWriter(), data);
    }

    /** Write a JSON error response. */
    public static void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", message);
        error.put("status", status);
        sendJson(resp, status, error);
    }

    /** Write a success message JSON response. */
    public static void sendSuccess(HttpServletResponse resp, String message) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("success", true);
        sendJson(resp, HttpServletResponse.SC_OK, body);
    }

    /** Parse a JSON request body into the given type. */
    public static <T> T parseBody(jakarta.servlet.http.HttpServletRequest req, Class<T> type) throws IOException {
        return MAPPER.readValue(req.getReader(), type);
    }

    /** Return the shared ObjectMapper (e.g., for custom serialisation). */
    public static ObjectMapper mapper() { return MAPPER; }
}
