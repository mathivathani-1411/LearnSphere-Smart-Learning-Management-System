package com.lms.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database connection pool using HikariCP (lightweight) or a simple
 * DriverManager-based fallback when HikariCP is not on classpath.
 *
 * Connection settings are read from environment variables (recommended) or
 * from the compiled-in defaults (only for local development).
 *
 * Environment variables:
 *   DB_URL      — jdbc:mysql://localhost:3306/lms
 *   DB_USER     — lms_user
 *   DB_PASSWORD — secret
 */
public class DBConnection {

    private static final Logger log = LoggerFactory.getLogger(DBConnection.class);

    private static String URL;
    private static String USER;
    private static String PASS;

    private static HikariDataSource dataSource;

    static {
        java.util.Properties props = new java.util.Properties();
        try (java.io.InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            log.warn("Could not load application.properties: {}", e.getMessage());
        }

        String envUrl = System.getenv("DB_URL");
        String envUser = System.getenv("DB_USER");
        String envPass = System.getenv("DB_PASSWORD");

        URL = envUrl != null ? envUrl : props.getProperty("db.url", "jdbc:mysql://localhost:3306/lms?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
        USER = envUser != null ? envUser : props.getProperty("db.user", "root");
        PASS = envPass != null ? envPass : props.getProperty("db.password", "root");
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(URL);
            config.setUsername(USER);
            config.setPassword(PASS);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30_000);
            config.setIdleTimeout(600_000);
            config.setMaxLifetime(1_800_000);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            dataSource = new HikariDataSource(config);
            log.info("HikariCP connection pool initialised: {}", URL);
        } catch (Exception e) {
            log.error("Failed to initialise connection pool: {}", e.getMessage(), e);
            // Fall back to null; getConnection() will use DriverManager
        }
    }

    /** Returns a connection from the pool (or via DriverManager as fallback). */
    public static Connection getConnection() throws SQLException {
        if (dataSource != null) {
            return dataSource.getConnection();
        }
        // Fallback — used in tests or when pool init failed
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("MySQL JDBC driver not found", ex);
        }
        return java.sql.DriverManager.getConnection(URL, USER, PASS);
    }

    /** Safely close resources. */
    public static void close(AutoCloseable... resources) {
        for (AutoCloseable res : resources) {
            if (res != null) {
                try { res.close(); } catch (Exception ignored) {}
            }
        }
    }
}
