package com.infotech.lang.application.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SentencePickerTest {

    @Test
    void skipsBlankLinesAndCommentsAndNumbersTheRest() {
        List<SentencePicker.Entry> entries = SentencePicker.parse(List.of("# a comment", "", "  I am a student.  ", "Come here."));
        assertEquals(2, entries.size());
        assertEquals("I am a student.", entries.get(0).english());
        assertEquals(1L, entries.get(0).id());
        assertEquals(2L, entries.get(1).id());
        assertTrue(entries.get(1).tamil().isEmpty());
    }

    @Test
    void readsCanonicalAndVariantAnswersPerLanguage() {
        List<SentencePicker.Entry> entries = SentencePicker.parse(List.of("I drink water. | நான் தண்ணீர் குடிக்கிறேன். | मैं पानी पीता हूँ।; मैं पानी पीती हूँ।"));
        TranslationItem hindi = entries.get(0).forLanguage("hindi");
        assertEquals("मैं पानी पीता हूँ।", hindi.expected());
        assertEquals(List.of("मैं पानी पीती हूँ।"), hindi.variants());
        assertEquals(2, hindi.accepted().size());
        TranslationItem tamil = entries.get(0).forLanguage("tamil");
        assertEquals("நான் தண்ணீர் குடிக்கிறேன்.", tamil.expected());
        assertTrue(tamil.variants().isEmpty());
    }

    @Test
    void theShippedListHasEnoughDifferentSentencesForTheBlock() {
        SentencePicker picker = new SentencePicker();
        for (String language : List.of("tamil", "hindi")) {
            List<TranslationItem> picked = picker.pick(language, 8);
            assertEquals(8, picked.size());
            Set<Long> ids = new HashSet<>();
            picked.forEach(i -> ids.add(i.id()));
            assertEquals(8, ids.size());
            assertTrue(picked.stream().allMatch(i -> !i.english().isBlank()));
        }
    }

    @Test
    void sentencesAtEvenPositionsHaveAnAnswerForTheTileQuestions() {
        SentencePicker picker = new SentencePicker();
        for (String language : List.of("tamil", "hindi")) {
            List<TranslationItem> picked = picker.pick(language, 8);
            for (int i = 0; i < picked.size(); i += 2) {
                assertTrue(picked.get(i).hasAnswer(), language + " position " + i);
            }
        }
    }

    @Test
    void everyShippedSentenceHasATamilAnswer() {
        SentencePicker picker = new SentencePicker();
        assertFalse(picker.allFor("tamil").isEmpty());
        assertTrue(picker.allFor("tamil").stream().allMatch(TranslationItem::hasAnswer));
    }

    @Test
    void askingForMoreThanThereAreReturnsAll() {
        assertTrue(new SentencePicker().pick("hindi", 1000).size() >= 20);
    }
}
