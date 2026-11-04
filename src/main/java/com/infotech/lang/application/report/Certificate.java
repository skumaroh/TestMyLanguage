package com.infotech.lang.application.report;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Who earns a certificate and what is printed on it. Decided in Java from the stored report numbers only (never by a model): a full
 * assessment, completed, with an overall score of {@value #THRESHOLD_PERCENT}% or more.
 */
public final class Certificate {

    public static final double THRESHOLD_PERCENT = 90.0;

    /** The plain wording that stops the page being mistaken for an accredited qualification. */
    public static final String DISCLAIMER = "This certificate records the result of an AI-guided placement assessment taken on "
            + "TestMyLanguage. It is not an accredited qualification.";

    private Certificate() {
    }

    public static boolean eligible(ReportSnapshot r) {
        return r != null && r.fullAssessment() && r.percent() >= THRESHOLD_PERCENT - 1e-9;
    }

    /** For example {@code TML-2026-000123}: the year of completion and the attempt number. */
    public static String number(ReportSnapshot r) {
        String year = r.completedAt() != null && r.completedAt().length() >= 4 && r.completedAt().substring(0, 4).chars().allMatch(Character::isDigit)
                ? r.completedAt().substring(0, 4) : "0000";
        return String.format(Locale.ROOT, "TML-%s-%06d", year, r.attemptId());
    }

    /** The completion date as "3 November 2026", or an empty text when it cannot be read. */
    public static String dateText(ReportSnapshot r) {
        try {
            LocalDate date = LocalDate.parse(r.completedAt().substring(0, 10));
            return date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH));
        } catch (RuntimeException e) {
            return "";
        }
    }

    /** "Level 1 (Foundation)" for the first level, otherwise "Level N". */
    public static String levelLabel(int level) {
        return level == 1 ? "Level 1 (Foundation)" : "Level " + level;
    }

    public static String languageName(ReportSnapshot r) {
        return "hindi".equalsIgnoreCase(r.language()) ? "Hindi" : "Tamil";
    }

    public static final int MAX_NAME_PART = 100;

    /** First and last name joined by one space; empty when either is missing (the certificate needs both). */
    public static String fullName(String firstName, String lastName) {
        String f = cleanNamePart(firstName);
        String l = cleanNamePart(lastName);
        return f.isEmpty() || l.isEmpty() ? "" : f + " " + l;
    }

    /** One part of a name: trimmed, inner spaces squeezed, control characters removed, at most {@value #MAX_NAME_PART} characters. */
    public static String cleanNamePart(String text) {
        if (text == null) {
            return "";
        }
        String t = text.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").trim();
        return t.length() > MAX_NAME_PART ? t.substring(0, MAX_NAME_PART).trim() : t;
    }
}
