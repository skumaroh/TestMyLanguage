package com.infotech.lang.application.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportMathTest {

    @Test
    void percentHasTwoDecimalsAndZeroWhenNothingWasAsked() {
        assertEquals(new BigDecimal("75.00"), ReportMath.percent(8, 6));
        assertEquals(new BigDecimal("66.67"), ReportMath.percent(6, 4));
        assertEquals(new BigDecimal("0.00"), ReportMath.percent(0, 0));
    }

    @Test
    void theLevelPassesAtSeventyFivePercentOverall() {
        assertTrue(ReportMath.reachesPassMark(20, 15));
        assertFalse(ReportMath.reachesPassMark(20, 14));
        assertTrue(ReportMath.reachesPassMark(4, 3));
        assertTrue(ReportMath.reachesPassMark(28, 21));
        assertFalse(ReportMath.reachesPassMark(0, 0));
    }

    @Test
    void aPartialAttemptHasNoLevelResult() {
        assertNull(ReportMath.levelPassed(false, 8, 8));
        assertEquals(Boolean.TRUE, ReportMath.levelPassed(true, 20, 16));
        assertEquals(Boolean.FALSE, ReportMath.levelPassed(true, 20, 10));
    }

    @Test
    void fullMeansEveryBlockOfTheAssessmentWasTaken() {
        assertTrue(ReportMath.isFull(List.of("WORDS", "DRAW_LETTERS", "AUDIO_WORDS", "TRANSLATION", "CONVERSATION")));
        assertFalse(ReportMath.isFull(List.of("WORDS", "CONVERSATION")));
        assertFalse(ReportMath.isFull(List.of()));
    }

    @Test
    void theMessagesAreKindAndNeverHarsh() {
        assertTrue(ReportMath.levelMessage(true, 1, true).contains("Level 2"));
        String below = ReportMath.levelMessage(false, 1, true);
        assertTrue(below.contains("Keep practising"));
        assertFalse(below.toLowerCase().contains("fail"));
        assertTrue(ReportMath.levelMessage(null, 1, false).contains("partial"));
    }
}
