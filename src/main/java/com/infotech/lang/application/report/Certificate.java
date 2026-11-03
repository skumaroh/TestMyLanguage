package com.infotech.lang.application.report;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Who earns a certificate and what is printed on it. Decided in Java from the stored report numbers only (never by a model): a full
 * assessment, completed, with an overall score strictly above {@value #THRESHOLD_PERCENT}%.
 */
public final class Certificate {

    public static final double THRESHOLD_PERCENT = 90.0;

    /** The plain wording that stops the page being mistaken for an accredited qualification. */
    public static final String DISCLAIMER = "This certificate records the result of an AI-guided placement assessment taken on "
            + "TestMyLanguage. It is not an accredited qualification.";

    private Certificate() {
    }

    public static boolean eligible(ReportSnapshot r) {
        return r != null && r.fullAssessment() && r.percent() > THRESHOLD_PERCENT + 1e-9;
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

    public static String languageName(ReportSnapshot r) {
        return "hindi".equalsIgnoreCase(r.language()) ? "Hindi" : "Tamil";
    }

    /** The name on the certificate: the display name, else first and last name, else a neutral word. */
    public static String nameOf(String displayName, String firstName, String lastName) {
        if (displayName != null && !displayName.isBlank()) {
            return displayName.trim();
        }
        String full = ((firstName == null ? "" : firstName.trim()) + " " + (lastName == null ? "" : lastName.trim())).trim();
        return full.isEmpty() ? "Learner" : full;
    }
}
