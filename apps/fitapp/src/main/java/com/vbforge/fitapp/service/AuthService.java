package com.vbforge.fitapp.service;

import com.vbforge.fitapp.db.LoginHistoryDao;
import com.vbforge.fitapp.db.UserDao;
import com.vbforge.fitapp.model.User;
import com.vbforge.fitapp.session.SessionManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/** Registration, login, password management. Passwords are stored as SHA-256(salt + password). */
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserDao users;
    private final LoginHistoryDao history;
    private final SessionManager session;

    public AuthService(UserDao users, LoginHistoryDao history, SessionManager session) {
        this.users = users;
        this.history = history;
        this.session = session;
    }

    // ---- hashing ---------------------------------------------------------

    public static String generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public static boolean matches(String password, String salt, String expectedHash) {
        byte[] a = hash(password, salt).getBytes(StandardCharsets.UTF_8);
        byte[] b = expectedHash.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b); // constant-time comparison
    }

    // ---- use cases -------------------------------------------------------

    public User register(String username, String email, String password) {
        username = username == null ? null : username.trim();
        email = email == null ? null : email.trim();
        requireValid(Validator.username(username));
        requireValid(Validator.email(email));
        requireValid(Validator.password(password));
        try {
            if (users.findByUsername(username).isPresent()) throw new ServiceException("That username is already taken.");
            if (users.findByEmail(email).isPresent()) throw new ServiceException("That email is already registered.");
            String salt = generateSalt();
            User user = users.insert(username, email, hash(password, salt), salt);
            startSession(user);
            return user;
        } catch (SQLException e) {
            throw new ServiceException("Could not create the account: " + e.getMessage(), e);
        }
    }

    public User login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty())
            throw new ServiceException("Please enter your username and password.");
        try {
            User user = users.findByUsername(username.trim()).orElse(null);
            // same message for unknown user / wrong password so usernames can't be probed
            if (user == null || !matches(password, user.salt(), user.passwordHash()))
                throw new ServiceException("Invalid username or password.");
            startSession(user);
            return user;
        } catch (SQLException e) {
            throw new ServiceException("Login failed: " + e.getMessage(), e);
        }
    }

    public void logout() {
        session.logout();
    }

    public void changePassword(String oldPassword, String newPassword) {
        User user = session.getCurrentUser();
        if (oldPassword == null || !matches(oldPassword, user.salt(), user.passwordHash()))
            throw new ServiceException("Current password is incorrect.");
        requireValid(Validator.password(newPassword));
        if (oldPassword.equals(newPassword))
            throw new ServiceException("New password must differ from the current one.");
        try {
            String salt = generateSalt();
            String newHash = hash(newPassword, salt);
            users.updatePassword(user.id(), newHash, salt);
            session.login(new User(user.id(), user.username(), user.email(), newHash, salt, user.createdAt()));
        } catch (SQLException e) {
            throw new ServiceException("Could not change the password: " + e.getMessage(), e);
        }
    }

    public boolean emailExists(String email) {
        try {
            return email != null && users.findByEmail(email.trim()).isPresent();
        } catch (SQLException e) {
            throw new ServiceException("Lookup failed: " + e.getMessage(), e);
        }
    }

    /** Offline "forgot password": anyone with local access who knows the account email can reset it. */
    public void resetPassword(String email, String newPassword) {
        requireValid(Validator.password(newPassword));
        try {
            User user = users.findByEmail(email == null ? "" : email.trim())
                    .orElseThrow(() -> new ServiceException("No account found with that email."));
            String salt = generateSalt();
            users.updatePassword(user.id(), hash(newPassword, salt), salt);
        } catch (SQLException e) {
            throw new ServiceException("Could not reset the password: " + e.getMessage(), e);
        }
    }

    public List<LocalDateTime> recentLogins(int limit) {
        try {
            return history.recent(session.getUserId(), limit);
        } catch (SQLException e) {
            throw new ServiceException("Could not load login history: " + e.getMessage(), e);
        }
    }

    // ---- helpers ---------------------------------------------------------

    private void startSession(User user) throws SQLException {
        history.record(user.id());
        session.login(user);
    }

    private static void requireValid(String error) {
        if (error != null) throw new ServiceException(error);
    }
}
