package com.vbforge.fitapp.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorTest {

    @Test
    void usernameRules() {
        assertNull(Validator.username("john_doe"));
        assertNotNull(Validator.username("ab"));
        assertNotNull(Validator.username("a".repeat(21)));
        assertNotNull(Validator.username("bad name"));
        assertNotNull(Validator.username(""));
        assertNotNull(Validator.username(null));
    }

    @Test
    void emailRules() {
        assertNull(Validator.email("a@b.com"));
        assertNull(Validator.email("first.last+tag@sub.example.org"));
        assertNotNull(Validator.email("no-at-sign"));
        assertNotNull(Validator.email("a@b"));
        assertNotNull(Validator.email(" "));
    }

    @Test
    void passwordStrength() {
        assertNull(Validator.password("abcd1234"));
        assertNotNull(Validator.password("short1"));
        assertNotNull(Validator.password("onlyletters"));
        assertNotNull(Validator.password("12345678"));
        assertNotNull(Validator.password(""));
    }

    @Test
    void setsAndReps() {
        assertNull(Validator.sets(3));
        assertNull(Validator.reps(10));
        assertNotNull(Validator.sets(0));
        assertNotNull(Validator.sets(-1));
        assertNotNull(Validator.reps(0));
        assertNotNull(Validator.reps(1000));
    }

    @Test
    void parseInt() {
        assertEquals(12, Validator.parseInt(" 12 "));
        assertEquals(-1, Validator.parseInt("abc"));
        assertEquals(-1, Validator.parseInt(""));
    }
}
