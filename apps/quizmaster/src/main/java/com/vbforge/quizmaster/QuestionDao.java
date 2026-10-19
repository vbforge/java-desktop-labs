package com.vbforge.quizmaster;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionDao {
    private final Db db;

    public QuestionDao(Db db) {
        this.db = db;
        seedIfEmpty();
    }

    public List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        try (Statement s = db.conn().createStatement();
             ResultSet rs = s.executeQuery("SELECT DISTINCT category FROM questions ORDER BY category")) {
            while (rs.next()) categories.add(rs.getString("category"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }

    public List<Question> getQuestionsByCategory(String category) {
        List<Question> list = new ArrayList<>();
        try (PreparedStatement ps = db.conn().prepareStatement(
                "SELECT * FROM questions WHERE category = ?")) {
            ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Question(
                            rs.getString("category"),
                            rs.getString("text"),
                            List.of(rs.getString("option_a"), rs.getString("option_b"), 
                                    rs.getString("option_c"), rs.getString("option_d")),
                            rs.getInt("correct_index")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int addQuestions(List<Question> questions) {
        int added = 0;
        try (PreparedStatement ps = db.conn().prepareStatement(
                // CHANGED TO: INSERT OR IGNORE
                "INSERT OR IGNORE INTO questions(category, text, option_a, option_b, option_c, option_d, correct_index) VALUES (?, ?, ?, ?, ?, ?, ?)")) {

            db.conn().setAutoCommit(false);
            for (Question q : questions) {
                ps.setString(1, q.category());
                ps.setString(2, q.text());
                ps.setString(3, q.options().get(0));
                ps.setString(4, q.options().get(1));
                ps.setString(5, q.options().get(2));
                ps.setString(6, q.options().get(3));
                ps.setInt(7, q.correctIndex());
                ps.addBatch();
            }

            // executeBatch returns an array of update counts (1 = inserted, 0 = ignored)
            int[] results = ps.executeBatch();
            for (int res : results) {
                if (res > 0) added += res;
            }

            db.conn().commit();
            db.conn().setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return added;
    }

    private void seedIfEmpty() {
        try (Statement s = db.conn().createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM questions")) {
            if (rs.next() && rs.getLong(1) > 0) return;
        } catch (SQLException e) {
            return;
        }

        List<Question> seed = List.of(
                new Question("Java", "Which of these is not a primitive type in Java?", List.of("String", "int", "boolean", "char"), 0),
                new Question("Java", "What is the default value of a boolean?", List.of("true", "false", "null", "0"), 1),
                new Question("Java", "Which keyword is used to inherit a class?", List.of("implements", "inherits", "extends", "super"), 2),
                new Question("Animals", "What is the fastest land animal?", List.of("Lion", "Cheetah", "Gazelle", "Horse"), 1),
                new Question("Animals", "What do you call a group of wolves?", List.of("Pack", "Herd", "Flock", "School"), 0),
                new Question("General", "How many continents are there?", List.of("5", "6", "7", "8"), 2)
        );
        addQuestions(seed);
    }
}