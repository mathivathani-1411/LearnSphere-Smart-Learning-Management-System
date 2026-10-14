package com.lms.service;

import com.lms.dao.UserDAO;
import com.lms.model.User;
import com.lms.util.PasswordUtil;
import com.lms.util.ValidationUtil;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Business logic for authentication and user management.
 */
public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    /** For testing: inject a custom DAO. */
    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Register a new user.
     *
     * @throws IllegalArgumentException if input is invalid or email is taken.
     * @throws SQLException             on database error.
     */
    public User register(String name, String email, String password, String role) throws SQLException {
        // Validate inputs
        name     = ValidationUtil.sanitise(name);
        email    = ValidationUtil.sanitise(email);
        password = password == null ? null : password.trim();

        if (!ValidationUtil.isValidName(name)) {
            throw new IllegalArgumentException("Name must be 1–150 characters.");
        }
        if (!ValidationUtil.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email address.");
        }
        if (!ValidationUtil.isValidPassword(password)) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
        if (!"STUDENT".equalsIgnoreCase(role) && !"INSTRUCTOR".equalsIgnoreCase(role)) {
            throw new IllegalArgumentException("Role must be STUDENT or INSTRUCTOR.");
        }

        // Check duplicate email
        if (userDAO.emailExists(email)) {
            throw new IllegalArgumentException("Email is already registered.");
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email.toLowerCase());
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setRole(role.toUpperCase());

        int id = userDAO.create(user);
        if (id < 0) throw new SQLException("Failed to create user.");
        user.setUserId(id);
        return user;
    }

    /**
     * Authenticate a user by email and password.
     *
     * @return the User if credentials are valid.
     * @throws IllegalArgumentException if credentials are wrong.
     * @throws SQLException             on database error.
     */
    public User login(String email, String password) throws SQLException {
        if (email == null || password == null) {
            throw new IllegalArgumentException("Email and password are required.");
        }
        email = email.trim().toLowerCase();

        Optional<User> userOpt = userDAO.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        User user = userOpt.get();
        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }
        return user;
    }

    /** Look up a user by ID (e.g., to refresh session data). */
    public Optional<User> findById(int userId) throws SQLException {
        return userDAO.findById(userId);
    }
}
