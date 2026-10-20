package com.vbforge.temptrack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CityDaoTest {
    private CityDao dao;

    @BeforeEach
    void setUp() {
        dao = new CityDao(":memory:");
    }

    @AfterEach
    void tearDown() {
        dao.close();
    }

    @Test
    void testSaveAndFind() {
        dao.save(new City(0, "London", "UK", 51.5, -0.1));
        dao.save(new City(0, "Tokyo", "Japan", 35.6, 139.6));
        
        List<City> cities = dao.findAll();
        assertEquals(2, cities.size());
        assertEquals("London", cities.get(0).name());
        assertEquals("Tokyo", cities.get(1).name());
    }

    @Test
    void testDelete() {
        dao.save(new City(0, "London", "UK", 51.5, -0.1));
        City london = dao.findAll().get(0);
        
        dao.delete(london.id());
        assertTrue(dao.findAll().isEmpty());
    }
}