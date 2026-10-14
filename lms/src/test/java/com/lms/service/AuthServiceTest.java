package com.lms.service;

import com.lms.dao.UserDAO;
import com.lms.model.User;
import com.lms.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserDAO userDAO;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userDAO);
    }

    @Test
    @DisplayName("Successful user registration hashes password and returns User")
    void testRegisterSuccess() throws SQLException {
        when(userDAO.emailExists("alice@lms.com")).thenReturn(false);
        when(userDAO.create(any(User.class))).thenReturn(101);

        User registered = authService.register("Alice Smith", "alice@lms.com", "secure123", "STUDENT");

        assertNotNull(registered);
        assertEquals(101, registered.getUserId());
        assertEquals("alice@lms.com", registered.getEmail());
        assertEquals("STUDENT", registered.getRole());
        assertTrue(PasswordUtil.verify("secure123", registered.getPasswordHash()));

        verify(userDAO, times(1)).create(any(User.class));
    }

    @Test
    @DisplayName("Registration throws exception if email is already registered")
    void testRegisterDuplicateEmail() throws SQLException {
        when(userDAO.emailExists("duplicate@lms.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            authService.register("Duplicate User", "duplicate@lms.com", "password123", "STUDENT")
        );

        assertTrue(ex.getMessage().contains("already registered"));
        verify(userDAO, never()).create(any(User.class));
    }

    @Test
    @DisplayName("Registration rejects passwords shorter than 6 characters")
    void testRegisterShortPassword() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            authService.register("Short Pass", "short@lms.com", "123", "STUDENT")
        );
        assertTrue(ex.getMessage().contains("at least 6 characters"));
    }

    @Test
    @DisplayName("Registration rejects invalid roles")
    void testRegisterInvalidRole() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            authService.register("Bad Role", "bad@lms.com", "password123", "ADMINISTRATOR")
        );
        assertTrue(ex.getMessage().contains("Role must be STUDENT or INSTRUCTOR"));
    }

    @Test
    @DisplayName("Login succeeds with valid email and password")
    void testLoginSuccess() throws SQLException {
        User dbUser = new User();
        dbUser.setUserId(42);
        dbUser.setName("Bob Jones");
        dbUser.setEmail("bob@lms.com");
        dbUser.setPasswordHash(PasswordUtil.hash("mypassword"));
        dbUser.setRole("INSTRUCTOR");

        when(userDAO.findByEmail("bob@lms.com")).thenReturn(Optional.of(dbUser));

        User loggedIn = authService.login("bob@lms.com", "mypassword");

        assertNotNull(loggedIn);
        assertEquals(42, loggedIn.getUserId());
        assertEquals("INSTRUCTOR", loggedIn.getRole());
    }

    @Test
    @DisplayName("Login fails with incorrect password")
    void testLoginWrongPassword() throws SQLException {
        User dbUser = new User();
        dbUser.setUserId(42);
        dbUser.setEmail("bob@lms.com");
        dbUser.setPasswordHash(PasswordUtil.hash("mypassword"));

        when(userDAO.findByEmail("bob@lms.com")).thenReturn(Optional.of(dbUser));

        assertThrows(IllegalArgumentException.class, () ->
            authService.login("bob@lms.com", "wrongpassword")
        );
    }

    @Test
    @DisplayName("Login fails if user does not exist")
    void testLoginUserNotFound() throws SQLException {
        when(userDAO.findByEmail("nonexistent@lms.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            authService.login("nonexistent@lms.com", "somepassword")
        );
    }
}
