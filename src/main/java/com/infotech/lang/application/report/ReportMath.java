package com.infotech.lang.application.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/** The numbers and wording of the final report. Java decides all of it; the model only writes the friendly summary. */
public final class ReportMath {

    /** The five parts that exist whatever the video library holds (words, letters, audio words, translation, conversation). */
    public static final List<String> CORE_ASSESSMENT = List.of("WORDS", "DRAW_LETTERS", "AUDIO_WORDS", "TRANSLATION", "CONVERSATION");

    /** The parts of a full assessment: the five core parts and the video part (owner decision, 2026-11-03: video is not optional). */
    public static final List<String> FULL_ASSESSMENT = List.of("WORDS", "DRAW_LETTERS", "AUDIO_WORDS", "TRANSLATION", "CONVERSATION", "VIDEO");

    /** The overall percentage that passes the level (owner decision). */
    public static final double PASS_RATIO = 0.75;

    private static final Map<String, String> TITLES = Map.of(
            "WORDS", "Words",
            "DRAW_LETTERS", "Writing letters",
            "AUDIO_WORDS", "Listening to words",
            "TRANSLATION", "Translation",
            "CONVERSATION", "Conversation",
            "VIDEO", "Video");

    private ReportMath() {
    }

    public static String title(String blockCode) {
        return TITLES.getOrDefault(blockCode, blockCode);
    }

    /** Overall percent with two decimals; 0 when nothing was asked. */
    public static BigDecimal percent(int asked, int correct) {
        if (asked <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(100.0 * correct / asked).setScale(2, RoundingMode.HALF_UP);
    }

    /** True when at least 75% of all questions asked were correct. */
    public static boolean reachesPassMark(int asked, int correct) {
        return asked > 0 && correct >= Math.ceil(PASS_RATIO * asked - 1e-9);
    }

    /** All six parts of a full assessment were taken. */
    public static boolean isFull(List<String> blockCodesTaken) {
        return blockCodesTaken.containsAll(FULL_ASSESSMENT);
    }

    /**
     * Same, with one safety net: while the video library has too few verified clips for the video part to run at all, the five core
     * parts make a full assessment, so learners are not left without a result because of missing content.
     */
    public static boolean isFull(List<String> blockCodesTaken, boolean videoAvailable) {
        return blockCodesTaken.containsAll(CORE_ASSESSMENT) && (blockCodesTaken.contains("VIDEO") || !videoAvailable);
    }

    /** The level result: null for a partial attempt (not every block was taken), otherwise passed or not. */
    public static Boolean levelPassed(boolean full, int asked, int correct) {
        return full ? reachesPassMark(asked, correct) : null;
    }

    public static String levelMessage(Boolean levelPassed, int level, boolean full) {
        if (!full) {
            return "This was a partial assessment (not every part was taken), so there is no level result.";
        }
        if (Boolean.TRUE.equals(levelPassed)) {
            return "Well done! You have completed Level " + level + ". Your next assessment is Level " + (level + 1) + ".";
        }
        return "You have completed Level " + level + ". Keep practising Level " + level + " and try again: every attempt helps you learn.";
    }
}
