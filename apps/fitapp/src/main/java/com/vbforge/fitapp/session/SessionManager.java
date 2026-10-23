package com.vbforge.fitapp.session;

import com.vbforge.fitapp.model.User;

/** Holds the currently logged-in user (replaces the old static Utils.currentPerson). */
public class SessionManager {

    private User currentUser;

    public void login(User user) {
        this.currentUser = user;
    }

    public void logout() {
        this.currentUser = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        if (currentUser == null) {
            throw new IllegalStateException("No user is logged in");
        }
        return currentUser;
    }

    public long getUserId() {
        return getCurrentUser().id();
    }
}
