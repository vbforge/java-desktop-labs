package com.vbforge.dietarytrack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DietaryDaoTest {

    private static DietaryEntry entry(String date, String first, int water, int meals) {
        return new DietaryEntry(0, date, first, "Doe", "+380501234567", "a@b.co", "Male",
                water, meals, true, false, true, "1 - 2 Miles", 72.5);
    }

    @Test
    void insertAndFindAllRoundTrip() {
        try (DietaryDao dao = new DietaryDao(DietaryDao.IN_MEMORY)) {
            dao.insert(entry("2026-10-01", "Ann", 8, 3));
            List<DietaryEntry> all = dao.findAll();
            assertEquals(1, all.size());
            DietaryEntry e = all.get(0);
            assertTrue(e.id() > 0);
            assertEquals("Ann", e.firstName());
            assertEquals("2026-10-01", e.date());
            assertEquals(8, e.water());
            assertTrue(e.wheat());
            assertFalse(e.sugar());
            assertTrue(e.dairy());
            assertEquals(72.5, e.weight());
        }
    }

    @Test
    void newestDateFirstThenNewestId() {
        try (DietaryDao dao = new DietaryDao(DietaryDao.IN_MEMORY)) {
            dao.insert(entry("2026-10-01", "Old", 1, 1));
            dao.insert(entry("2026-10-03", "NewA", 1, 1));
            dao.insert(entry("2026-10-03", "NewB", 1, 1));
            List<String> names = dao.findAll().stream().map(DietaryEntry::firstName).toList();
            assertEquals(List.of("NewB", "NewA", "Old"), names);
        }
    }

    @Test
    void createsMissingParentDirectories(@TempDir Path tmp) {
        Path db = tmp.resolve("nested").resolve("dir").resolve("test.db");
        try (DietaryDao dao = new DietaryDao(db.toString())) {
            dao.insert(entry("2026-10-01", "Ann", 1, 1));
        }
        assertTrue(db.toFile().exists());
        try (DietaryDao reopened = new DietaryDao(db.toString())) {
            assertEquals(1, reopened.findAll().size());
        }
    }

    @Test
    void statsAverageWaterAndMeals() {
        Stats s = Stats.of(List.of(entry("2026-10-01", "A", 8, 2), entry("2026-10-02", "B", 10, 4)));
        assertEquals(2, s.total());
        assertEquals(9.0, s.avgWater());
        assertEquals(3.0, s.avgMeals());
        assertEquals(Stats.EMPTY, Stats.of(List.of()));
    }
}
