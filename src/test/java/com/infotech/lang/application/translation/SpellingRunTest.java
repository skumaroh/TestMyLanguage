package com.infotech.lang.application.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SpellingRunTest {

    // "பால் வெல்லையாக இருக்கிரது" – three words at 0-4, 5-14, 15-25
    private static final List<TypedWord> WORDS = List.of(
            new TypedWord("paal", "பால்", 0, 4),
            new TypedWord("vellaiyaaka", "வெல்லையாக", 5, 14),
            new TypedWord("irukkirathu", "இருக்கிரது", 15, 25));

    @Test
    void oneChangedWordGivesOneRunForThatWordOnly() {
        List<SpellingRun> runs = SpellingRun.group(WORDS, List.of("பால்", "வெள்ளையாக", "இருக்கிரது"));
        assertEquals(1, runs.size());
        assertEquals(new SpellingRun(5, 14, "வெல்லையாக", "வெள்ளையாக"), runs.get(0));
    }

    @Test
    void neighbouringChangedWordsFormOneRunThatReplacesAllOfThem() {
        List<SpellingRun> runs = SpellingRun.group(WORDS, List.of("பால்", "வெள்ளையாக", "இருக்கிறது"));
        assertEquals(1, runs.size());
        assertEquals(5, runs.get(0).start());
        assertEquals(25, runs.get(0).end());
        assertEquals("வெல்லையாக இருக்கிரது", runs.get(0).currentText());
        assertEquals("வெள்ளையாக இருக்கிறது", runs.get(0).suggestion());
    }

    @Test
    void wordsThatAreNotNeighboursAreSeparateRuns() {
        List<SpellingRun> runs = SpellingRun.group(WORDS, List.of("பாலு", "வெல்லையாக", "இருக்கிறது"));
        assertEquals(2, runs.size());
        assertEquals(0, runs.get(0).start());
        assertEquals(15, runs.get(1).start());
    }

    @Test
    void aWordWithAHandEditedWordBetweenIsNotGroupedAcross() {
        // the middle word was edited by hand and is not in the list, so the gap is wider than one space
        List<TypedWord> gap = List.of(WORDS.get(0), WORDS.get(2));
        List<SpellingRun> runs = SpellingRun.group(gap, List.of("பாலு", "இருக்கிறது"));
        assertEquals(2, runs.size());
    }

    @Test
    void nothingToSuggestWhenSpellingsMatchOrAnswerIsUnusable() {
        assertTrue(SpellingRun.group(WORDS, List.of("பால்", "வெல்லையாக", "இருக்கிரது")).isEmpty());
        assertTrue(SpellingRun.group(WORDS, List.of("பால்")).isEmpty());
        assertTrue(SpellingRun.group(WORDS, List.of("milk", "x y", "")).isEmpty());
    }
}
