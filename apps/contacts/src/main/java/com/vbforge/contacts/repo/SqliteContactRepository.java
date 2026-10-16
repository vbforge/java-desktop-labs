package com.vbforge.contacts.repo;

import com.vbforge.contacts.Contact;
import com.vbforge.contacts.ContactRepository;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class SqliteContactRepository implements ContactRepository {

    private final Path file;

    public SqliteContactRepository(Path file) {
        this.file = file;
    }

    @Override
    public List<Contact> findAll() throws SQLException {
        try (Connection c = connect()) {
            ensureSchema(c);
            List<Contact> out = new ArrayList<>();
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("""
                     SELECT id, name, phone, email
                     FROM contacts
                     ORDER BY name COLLATE NOCASE
                 """)) {
                while (rs.next()) {
                    out.add(new Contact(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("phone"),
                            rs.getString("email")));
                }
            }
            return out;
        }
    }

    @Override
    public void saveAll(List<Contact> contacts) throws SQLException {
        try (Connection c = connect()) {
            ensureSchema(c);
            c.setAutoCommit(false);
            try {
                try (Statement s = c.createStatement()) {
                    s.executeUpdate("DELETE FROM contacts");
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO contacts(id, name, phone, email) VALUES (?, ?, ?, ?)")) {
                    for (Contact k : contacts) {
                        ps.setLong(1, k.id());
                        ps.setString(2, k.name());
                        ps.setString(3, k.phone());
                        ps.setString(4, k.email());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + file);
    }

    private static void ensureSchema(Connection c) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.execute("""
                CREATE TABLE IF NOT EXISTS contacts (
                    id    INTEGER PRIMARY KEY,
                    name  TEXT NOT NULL,
                    phone TEXT NOT NULL DEFAULT '',
                    email TEXT NOT NULL DEFAULT ''
                )
            """);
        }
    }

    @Override
    public String describe() {
        return "SQLite (" + file.getFileName() + ")";
    }
}