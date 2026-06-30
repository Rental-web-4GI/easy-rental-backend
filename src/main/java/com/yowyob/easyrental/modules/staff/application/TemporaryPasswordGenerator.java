package com.yowyob.easyrental.modules.staff.application;

import java.security.SecureRandom;

/**
 * Generates temporary passwords compliant with kernel policy (≥10 chars, mixed case, digit, symbol).
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
public final class TemporaryPasswordGenerator {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghjkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "!@#$%&*";
    private static final SecureRandom RANDOM = new SecureRandom();

    private TemporaryPasswordGenerator() {
    }

    public static String generate() {
        StringBuilder password = new StringBuilder(14);
        password.append(randomChar(UPPER));
        password.append(randomChar(LOWER));
        password.append(randomChar(DIGITS));
        password.append(randomChar(SYMBOLS));
        String all = UPPER + LOWER + DIGITS + SYMBOLS;
        for (int i = 4; i < 14; i++) {
            password.append(randomChar(all));
        }
        return shuffle(password.toString());
    }

    private static char randomChar(String alphabet) {
        return alphabet.charAt(RANDOM.nextInt(alphabet.length()));
    }

    private static String shuffle(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }
}
