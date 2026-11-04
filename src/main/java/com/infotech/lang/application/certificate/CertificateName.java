package com.infotech.lang.application.certificate;

import java.text.Normalizer;
import java.util.regex.Pattern;

/** The rules for the one name printed on a learner's certificates. Letters of any script, spaces, dots, hyphens and apostrophes only. */
public final class CertificateName {

    public static final int MIN = 2;
    public static final int MAX = 100;

    private static final Pattern ALLOWED = Pattern.compile("^[\\p{L}\\p{M}][\\p{L}\\p{M} .'’-]*$");

    private CertificateName() {
    }

    /** Composed form, single spaces, trimmed, at most {@value #MAX} characters. */
    public static String clean(String text) {
        if (text == null) {
            return "";
        }
        String t = Normalizer.normalize(text, Normalizer.Form.NFC).replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").trim();
        return t.length() > MAX + 1 ? t.substring(0, MAX + 1) : t;   // one over, so that the length check can see it was too long
    }

    /** A short message for the learner when the (cleaned) name is not acceptable, or null when it is. */
    public static String problem(String cleaned) {
        if (cleaned == null || cleaned.isEmpty()) {
            return "Please enter your name.";
        }
        if (cleaned.length() > MAX) {
            return "The name can be at most " + MAX + " characters.";
        }
        long letters = cleaned.codePoints().filter(Character::isLetter).count();
        if (letters < MIN) {
            return "Please enter your full name.";
        }
        if (!ALLOWED.matcher(cleaned).matches()) {
            return "Use letters, spaces, dots, hyphens and apostrophes only (no digits, links or symbols).";
        }
        if (Pattern.compile("[\\p{L}\\p{M}]\\.[\\p{L}\\p{M}]").matcher(cleaned).find()) {
            return "Put a space after a dot (for example \"S. K. Rao\"). Web addresses are not names.";
        }
        return null;
    }
}
