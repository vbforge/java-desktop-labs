package com.vbforge.fitapp.model;

public record User(long id, String username, String email, String passwordHash, String salt, String createdAt) {
}
