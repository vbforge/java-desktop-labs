package com.vbforge.focussprint;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class SessionDao {

    private final Db db;

    public SessionDao(Db db) {
        this.db = db;
    }

    public List<Session> findSince(Instant since) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement("""
                SELECT id, section_id, started_at, ended_at, seconds
                FROM sessions
                WHERE started_at >= ?
                ORDER BY started_at DESC
                """)) {
            ps.setLong(1, since.toEpochMilli());
            try (ResultSet rs = ps.executeQuery()) {
                List<Session> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new Session(
                            rs.getLong("id"),
                            rs.getLong("section_id"),
                            Instant.ofEpochMilli(rs.getLong("started_at")),
                            Instant.ofEpochMilli(rs.getLong("ended_at")),
                            rs.getLong("seconds")));
                }
                return out;
            }
        }
    }

    public Session insert(Session s) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement("""
                INSERT INTO sessions(section_id, started_at, ended_at, seconds)
                VALUES (?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, s.sectionId());
            ps.setLong(2, s.startedAt().toEpochMilli());
            ps.setLong(3, s.endedAt().toEpochMilli());
            ps.setLong(4, s.seconds());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Session(keys.getLong(1), s.sectionId(),
                        s.startedAt(), s.endedAt(), s.seconds());
            }
        }
    }
}