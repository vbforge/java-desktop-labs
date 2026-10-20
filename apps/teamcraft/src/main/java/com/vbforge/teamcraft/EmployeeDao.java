package com.vbforge.teamcraft;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDao {
    private final Db db;

    public EmployeeDao(Db db) {
        this.db = db;
    }

    public List<Employee> findAll() {
        List<Employee> list = new ArrayList<>();
        try (Statement s = db.conn().createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM employees ORDER BY name")) {
            while (rs.next()) {
                list.add(new Employee(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("job"),
                        rs.getInt("age"),
                        rs.getString("gender")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void insert(Employee emp) {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "INSERT INTO employees(name, job, age, gender) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, emp.name());
            ps.setString(2, emp.job());
            ps.setInt(3, emp.age());
            ps.setString(4, emp.gender());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void update(Employee emp) {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "UPDATE employees SET name=?, job=?, age=?, gender=? WHERE id=?")) {
            ps.setString(1, emp.name());
            ps.setString(2, emp.job());
            ps.setInt(3, emp.age());
            ps.setString(4, emp.gender());
            ps.setLong(5, emp.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(long id) {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "DELETE FROM employees WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}