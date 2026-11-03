package com.infotech.lang.application.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CertificateTest {

    private static ReportSnapshot report(double percent, boolean full, String completedAt, String language) {
        return new ReportSnapshot(123, language, 1, "2026-11-03T10:00:00Z", completedAt, 40, 38, percent, full ? Boolean.TRUE : null,
                full, "msg", List.of(), List.of(), null, null);
    }

    @Test
    void onlyAFullAssessmentOf90PercentOrMoreEarnsACertificate() {
        assertTrue(Certificate.eligible(report(90.0, true, "2026-11-03T12:00:00Z", "tamil")));
        assertTrue(Certificate.eligible(report(90.01, true, "2026-11-03T12:00:00Z", "tamil")));
        assertTrue(Certificate.eligible(report(100, true, "2026-11-03T12:00:00Z", "hindi")));
        assertFalse(Certificate.eligible(report(89.99, true, "2026-11-03T12:00:00Z", "tamil")));
        assertFalse(Certificate.eligible(report(97, false, "2026-11-03T12:00:00Z", "tamil")));
        assertFalse(Certificate.eligible(null));
    }

    @Test
    void theNumberDateAndLanguageComeFromTheStoredReport() {
        ReportSnapshot r = report(95, true, "2026-11-03T12:00:00Z", "hindi");
        assertEquals("TML-2026-000123", Certificate.number(r));
        assertEquals("3 November 2026", Certificate.dateText(r));
        assertEquals("Hindi", Certificate.languageName(r));
        ReportSnapshot odd = report(95, true, null, "tamil");
        assertEquals("TML-0000-000123", Certificate.number(odd));
        assertEquals("", Certificate.dateText(odd));
        assertEquals("Tamil", Certificate.languageName(odd));
    }

    @Test
    void theNameIsTheDisplayNameThenFirstAndLastThenLearner() {
        assertEquals("Asha K", Certificate.nameOf("  Asha K ", "A", "B"));
        assertEquals("Asha Rao", Certificate.nameOf(" ", "Asha", "Rao"));
        assertEquals("Asha", Certificate.nameOf(null, "Asha", null));
        assertEquals("Learner", Certificate.nameOf(null, null, null));
        assertTrue(Certificate.DISCLAIMER.contains("not an accredited qualification"));
    }
}
