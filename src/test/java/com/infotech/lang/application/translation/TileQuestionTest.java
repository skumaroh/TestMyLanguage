package com.infotech.lang.application.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TileQuestionTest {

    private static final TranslationItem WATER = new TranslationItem(1, "I drink water.", "मैं पानी पीता हूँ।",
            List.of("मैं पानी पीती हूँ।"));

    private static List<TranslationItem> pool() {
        return List.of(WATER,
                new TranslationItem(2, "The sun is hot.", "सूरज गर्म है।"),
                new TranslationItem(3, "The dog is big.", "कुत्ता बड़ा है।"),
                new TranslationItem(4, "The milk is white.", "दूध सफ़ेद है।"),
                new TranslationItem(5, "I go to school.", "मैं स्कूल जाता हूँ।"));
    }

    @Test
    void wordsDropFinalPunctuation() {
        assertEquals(List.of("मैं", "पानी", "पीता", "हूँ"), TileQuestion.words("मैं पानी पीता हूँ।"));
        assertEquals(List.of("I", "am"), TileQuestion.words("I am."));
    }

    @Test
    void tilesHoldEveryAnswerWordAndOnlyForeignDecoys() {
        TileQuestion q = TileQuestion.build(WATER, pool());
        assertEquals(4 + TileQuestion.DECOYS, q.tiles().size());
        assertTrue(q.tiles().containsAll(TileQuestion.words(WATER.expected())));
        Set<String> own = new HashSet<>(TileQuestion.words(WATER.expected()));
        own.addAll(TileQuestion.words(WATER.variants().get(0)));
        long decoys = q.tiles().stream().filter(t -> !own.contains(t)).count();
        assertEquals(TileQuestion.DECOYS, decoys);
    }

    @Test
    void theCanonicalOrderAndTheVariantAreCorrect() {
        assertTrue(TileQuestion.matches(WATER, List.of("मैं", "पानी", "पीता", "हूँ")));
        assertTrue(TileQuestion.matches(WATER, List.of("मैं", "पानी", "पीती", "हूँ")));
    }

    @Test
    void aDifferentOrderOrWordIsNotAnExactMatch() {
        assertFalse(TileQuestion.matches(WATER, List.of("पानी", "मैं", "पीता", "हूँ")));
        assertFalse(TileQuestion.matches(WATER, List.of("मैं", "पानी", "पीता", "है")));
        assertFalse(TileQuestion.matches(WATER, List.of()));
    }
}
