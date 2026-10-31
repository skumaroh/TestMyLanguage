package com.infotech.lang.application.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class TileBuilderTest {

    private static final List<String> POOL = List.of("அ", "ஆ", "இ", "ஈ", "உ", "ஊ", "எ", "ஏ", "ஐ", "ஒ", "ஓ", "ஔ", "க", "ங");

    @Test
    void splitTrimsAndDropsEmptiesAndDuplicates() {
        assertEquals(List.of("க", "ம", "ல"), TileBuilder.split(" க, ம ,,ல,க"));
        assertTrue(TileBuilder.split(null).isEmpty());
    }

    @Test
    void tilesContainEveryLetterOfTheWordAndFillUpToTheCount() {
        List<String> tiles = TileBuilder.tilesFor("க,ம,ல", "கமல", POOL, 8, new Random(1));
        assertEquals(8, tiles.size());
        assertEquals(8, new HashSet<>(tiles).size());
        assertTrue(tiles.containsAll(List.of("க", "ம", "ல")));
    }

    @Test
    void tilesNeverDropWordLettersEvenWhenTheWordHasMoreThanTheCount() {
        List<String> tiles = TileBuilder.tilesFor("a,b,c,d,e", "abcde", List.of("x"), 3, new Random(2));
        assertEquals(5, tiles.size());
        assertTrue(tiles.containsAll(List.of("a", "b", "c", "d", "e")));
    }

    @Test
    void fallsBackToTheCharactersOfTheAnswerWhenThereIsNoLetterList() {
        List<String> tiles = TileBuilder.tilesFor("", "cat", List.of(), 3, new Random(3));
        assertEquals(new HashSet<>(List.of("c", "a", "t")), new HashSet<>(tiles));
    }

    @Test
    void answerCheckIgnoresSpacesAndUnicodeCompositionDifferences() {
        assertTrue(WordQuestionService.isCorrect(" cat ", "cat"));
        assertTrue(WordQuestionService.isCorrect("é", "é"));
        assertEquals(false, WordQuestionService.isCorrect("cut", "cat"));
        assertEquals(false, WordQuestionService.isCorrect(null, "cat"));
    }
}
