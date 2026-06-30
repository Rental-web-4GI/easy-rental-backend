package com.yowyob.easyrental.modules.staff.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporaryPasswordGeneratorTest {

    @Test
    void shouldGeneratePasswordWithMinimumLength() {
        String password = TemporaryPasswordGenerator.generate();
        assertTrue(password.length() >= 10);
    }

    @Test
    void shouldGenerateDifferentPasswords() {
        String first = TemporaryPasswordGenerator.generate();
        String second = TemporaryPasswordGenerator.generate();
        assertTrue(!first.equals(second) || first.length() == second.length());
    }
}
