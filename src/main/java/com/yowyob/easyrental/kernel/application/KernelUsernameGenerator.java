package com.yowyob.easyrental.kernel.application;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Génère un identifiant utilisateur (username) Kernel qui respecte
 * strictement le pattern imposé par kernel-core :
 *
 * <pre>^[A-Za-z0-9](?:[A-Za-z0-9._-]{1,30}[A-Za-z0-9])$</pre>
 *
 * <p>Contraintes :
 * <ul>
 *   <li>Commence et finit par une lettre ou un chiffre (pas de séparateur en bout)</li>
 *   <li>Autorise à l'intérieur : lettres, chiffres, points, tirets, underscores</li>
 *   <li>Longueur totale : 3 à 32 caractères</li>
 * </ul>
 *
 * <p>Un email {@code aizakifujimoto@gmail.com} produit typiquement
 * {@code aizakifujimoto}. Si le résultat est vide, trop court ou déjà pris,
 * un suffix numérique est ajouté (ex. {@code aizakifujimoto-2}).
 *
 * <p>Cette classe est utilisée au moment du signup pour éviter que Kernel
 * ne rejette silencieusement notre username et ne génère un placeholder
 * {@code pending-<uuid>} à la place.
 */
public final class KernelUsernameGenerator {

    private static final Pattern INVALID_CHARS = Pattern.compile("[^A-Za-z0-9._-]");
    private static final Pattern TRIM_EDGES = Pattern.compile("^[._-]+|[._-]+$");
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 32;

    private KernelUsernameGenerator() {
        // utility
    }

    /**
     * Extrait la base username à partir d'un email (partie avant le {@code @})
     * et l'assainit pour respecter le pattern Kernel.
     */
    public static String fromEmail(String email) {
        if (email == null || email.isBlank()) {
            return fallback();
        }
        String local = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        return sanitize(local);
    }

    /**
     * Assainit une chaîne quelconque : minuscule, retire les accents, remplace
     * les caractères interdits par {@code -}, coupe à 32, garantit une longueur
     * minimale de 3.
     */
    public static String sanitize(String input) {
        if (input == null) {
            return fallback();
        }
        String s = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        s = s.toLowerCase(Locale.ROOT);
        s = INVALID_CHARS.matcher(s).replaceAll("-");
        s = TRIM_EDGES.matcher(s).replaceAll("");
        if (s.length() > MAX_LENGTH) {
            s = s.substring(0, MAX_LENGTH);
            s = TRIM_EDGES.matcher(s).replaceAll("");
        }
        if (s.length() < MIN_LENGTH) {
            return fallback();
        }
        return s;
    }

    /**
     * Ajoute un suffix numérique à une base pour gérer les collisions.
     * {@code withSuffix("aizakifujimoto", 2)} produit {@code aizakifujimoto-2}.
     * La longueur totale reste inférieure ou égale à 32.
     */
    public static String withSuffix(String base, int index) {
        String suffix = "-" + index;
        int maxBase = MAX_LENGTH - suffix.length();
        String trimmed = base.length() > maxBase ? base.substring(0, maxBase) : base;
        trimmed = TRIM_EDGES.matcher(trimmed).replaceAll("");
        return trimmed + suffix;
    }

    private static String fallback() {
        // Fallback improbable : rare car le sanitize aboutit presque toujours
        // à quelque chose d'utilisable. On génère un identifiant lisible et unique.
        return "user-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
