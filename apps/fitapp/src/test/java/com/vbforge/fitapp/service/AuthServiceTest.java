package com.vbforge.fitapp.service;

import com.vbforge.fitapp.db.Database;
import com.vbforge.fitapp.db.LoginHistoryDao;
import com.vbforge.fitapp.db.UserDao;
import com.vbforge.fitapp.model.User;
import com.vbforge.fitapp.session.SessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private Database db;
    private SessionManager session;
    private AuthService auth;

    @BeforeEach
    void setUp() throws Exception {
        db = Database.openInMemory();
        session = new SessionManager();
        auth = new AuthService(new UserDao(db), new LoginHistoryDao(db), session);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void hashingIsSaltedAndVerifiable() {
        String salt1 = AuthService.generateSalt();
        String salt2 = AuthService.generateSalt();
        assertNotEquals(salt1, salt2);

        String h = AuthService.hash("secret123", salt1);
        assertNotEquals("secret123", h);
        assertEquals(64, h.length());
        assertNotEquals(h, AuthService.hash("secret123", salt2));
        assertTrue(AuthService.matches("secret123", salt1, h));
        assertFalse(AuthService.matches("wrong123", salt1, h));
    }

    @Test
    void registerStoresHashNotPassword() {
        User u = auth.register("john", "john@example.com", "password1");
        assertNotEquals("password1", u.passwordHash());
        assertTrue(session.isLoggedIn());
        assertEquals("john", session.getCurrentUser().username());
    }

    @Test
    void registerRejectsDuplicatesAndBadInput() {
        auth.register("john", "john@example.com", "password1");
        assertThrows(ServiceException.class, () -> auth.register("JOHN", "other@example.com", "password1"));
        assertThrows(ServiceException.class, () -> auth.register("jane", "john@example.com", "password1"));
        assertThrows(ServiceException.class, () -> auth.register("jane", "bad-email", "password1"));
        assertThrows(ServiceException.class, () -> auth.register("jane", "jane@example.com", "weak"));
    }

    @Test
    void loginSucceedsAndFailsCorrectly() {
        auth.register("john", "john@example.com", "password1");
        auth.logout();
        assertFalse(session.isLoggedIn());

        assertThrows(ServiceException.class, () -> auth.login("john", "nope12345"));
        assertThrows(ServiceException.class, () -> auth.login("ghost", "password1"));
        assertFalse(session.isLoggedIn());

        auth.login("john", "password1");
        assertTrue(session.isLoggedIn());
    }

    @Test
    void loginHistoryKeepsLatestFive() {
        auth.register("john", "john@example.com", "password1"); // login #1
        for (int i = 0; i < 6; i++) {
            auth.logout();
            auth.login("john", "password1");
        }
        assertEquals(5, auth.recentLogins(5).size());
    }

    @Test
    void changePasswordRequiresOldPassword() {
        auth.register("john", "john@example.com", "password1");
        assertThrows(ServiceException.class, () -> auth.changePassword("wrongold1", "newpass123"));
        assertThrows(ServiceException.class, () -> auth.changePassword("password1", "weak"));

        auth.changePassword("password1", "newpass123");
        auth.logout();
        assertThrows(ServiceException.class, () -> auth.login("john", "password1"));
        auth.login("john", "newpass123");
        assertTrue(session.isLoggedIn());
    }

    @Test
    void forgotPasswordResetsByEmail() {
        auth.register("john", "john@example.com", "password1");
        auth.logout();
        assertTrue(auth.emailExists("john@example.com"));
        assertFalse(auth.emailExists("nobody@example.com"));

        assertThrows(ServiceException.class, () -> auth.resetPassword("nobody@example.com", "newpass123"));
        auth.resetPassword("john@example.com", "newpass123");
        auth.login("john", "newpass123");
        assertTrue(session.isLoggedIn());
    }
}
