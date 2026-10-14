package com.lms.util;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

/**
 * Application Lifecycle Listener.
 * Verifies database connectivity and directory configurations upon deployment.
 */
@WebListener
public class AppInitListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(AppInitListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("==================================================");
        log.info(" Learning Management System (LMS) Initializing... ");
        log.info("==================================================");

        try (Connection conn = DBConnection.getConnection()) {
            log.info(" Database connection verified successfully: {}", conn.getMetaData().getDatabaseProductName());
        } catch (Exception e) {
            log.warn(" Database connection check: MySQL not accessible or not configured yet ({}).", e.getMessage());
            log.warn(" Tip: Ensure MySQL 8.0 is running and run 'sql/01_schema.sql' and 'sql/02_seed_data.sql'.");
        }

        String uploadDir = System.getenv("UPLOAD_DIR");
        if (uploadDir == null) {
            uploadDir = System.getProperty("catalina.home", System.getProperty("user.home")) + "/lms-uploads";
        }
        log.info(" File upload directory configured at: {}", uploadDir);
        log.info(" LMS Application initialized and ready.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("LMS Application shutting down...");
    }
}
