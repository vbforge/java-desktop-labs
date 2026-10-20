package com.vbforge.teamcraft;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EmployeeDaoTest {

    private Db db;
    private EmployeeDao dao;

    @BeforeEach
    void setUp() {
        // Use an in-memory database for testing
        db = new Db(":memory:");
        dao = new EmployeeDao(db);
    }

    @Test
    void testInsertAndFindAll() {
        dao.insert(new Employee(0, "Alice", "Engineer", 30, "Female"));
        dao.insert(new Employee(0, "Bob", "Manager", 45, "Male"));

        List<Employee> employees = dao.findAll();
        assertEquals(2, employees.size());
        assertEquals("Alice", employees.get(0).name());
        assertEquals("Bob", employees.get(1).name());
    }

    @Test
    void testUpdate() {
        dao.insert(new Employee(0, "Alice", "Engineer", 30, "Female"));
        Employee alice = dao.findAll().get(0);
        
        dao.update(new Employee(alice.id(), "Alice Smith", "Senior Engineer", 31, "Female"));
        
        Employee updated = dao.findAll().get(0);
        assertEquals("Alice Smith", updated.name());
        assertEquals("Senior Engineer", updated.job());
        assertEquals(31, updated.age());
    }

    @Test
    void testDelete() {
        dao.insert(new Employee(0, "Alice", "Engineer", 30, "Female"));
        Employee alice = dao.findAll().get(0);
        
        dao.delete(alice.id());
        assertTrue(dao.findAll().isEmpty());
    }
}