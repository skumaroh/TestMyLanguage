package com.infotech.lang.application.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SentencePickerTest {

    @Test
    void skipsBlankLinesAndCommentsAndNumbersTheRest() {
        List<TranslationItem> items = SentencePicker.parse(List.of("# a comment", "", "  I am a student.  ", "Come here."));
        assertEquals(2, items.size());
        assertEquals("I am a student.", items.get(0).english());
        assertEquals(1L, items.get(0).id());
        assertEquals(2L, items.get(1).id());
        assertEquals("", items.get(1).expected());
    }

    @Test
    void theShippedListHasEnoughDifferentSentencesForTheBlock() {
        SentencePicker picker = new SentencePicker();
        List<TranslationItem> picked = picker.pick("tamil", 8);
        assertEquals(8, picked.size());
        Set<Long> ids = new HashSet<>();
        picked.forEach(i -> ids.add(i.id()));
        assertEquals(8, ids.size());
        assertTrue(picked.stream().allMatch(i -> !i.english().isBlank()));
    }

    @Test
    void askingForMoreThanThereAreReturnsAll() {
        assertTrue(new SentencePicker().pick("hindi", 1000).size() >= 20);
    }
}
