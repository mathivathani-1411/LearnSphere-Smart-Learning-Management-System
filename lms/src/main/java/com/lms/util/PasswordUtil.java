package com.lms.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Password utilities using BCrypt.
 * Never store plain-text passwords — always hash before saving.
 */
public class PasswordUtil {

    private static final int BCRYPT_ROUNDS = 12;

    private PasswordUtil() {}

    /**
     * Hash a plain-text password using BCrypt.
     *
     * @param plainPassword the user-supplied password
     * @return the BCrypt hash string to store in the database
     */
    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    /**
     * Check whether a plain-text password matches a stored BCrypt hash.
     *
     * @param plainPassword the candidate password
     * @param storedHash    the hash stored in the database
     * @return true if they match
     */
    public static boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) return false;
        try {
            return BCrypt.checkpw(plainPassword, storedHash);
        } catch (Exception e) {
            return false;
        }
    }
}
