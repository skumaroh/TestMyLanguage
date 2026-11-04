package com.infotech.lang.application.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.infotech.lang.application.assessment.Modules;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModulesTest {

    @Test
    void sixModulesInOrderWithHeadings() {
        assertEquals(6, Modules.values().length);
        assertEquals("Module 1 – Picture, word and letter recognition", Modules.WORDS.heading());
        assertEquals(Optional.of(Modules.DRAW_LETTERS), Modules.WORDS.following());
        assertEquals(Optional.empty(), Modules.VIDEO.following());
    }

    @Test
    void nextModuleSkipsFinishedOnesAndVideoWhenNoClips() {
        assertEquals(Optional.of(Modules.WORDS), Modules.firstUnfinished(Set.of(), true));
        assertEquals(Optional.of(Modules.TRANSLATION), Modules.firstUnfinished(Set.of("WORDS", "DRAW_LETTERS", "AUDIO_WORDS"), true));
        assertEquals(Optional.of(Modules.VIDEO), Modules.firstUnfinished(Set.of("WORDS", "DRAW_LETTERS", "AUDIO_WORDS", "TRANSLATION", "CONVERSATION"), true));
        assertEquals(Optional.empty(), Modules.firstUnfinished(Set.of("WORDS", "DRAW_LETTERS", "AUDIO_WORDS", "TRANSLATION", "CONVERSATION"), false));
        assertFalse(Modules.taken(false).contains(Modules.VIDEO));
        assertTrue(Modules.taken(true).contains(Modules.VIDEO));
    }

    @Test
    void greetingAddressesReturningStudentsByName() {
        assertTrue(HomeView.greeting("Priya", true).startsWith("Welcome back, Priya."));
        assertTrue(HomeView.greeting("Priya", false).startsWith("Welcome, Priya."));
        assertTrue(HomeView.greeting("", true).startsWith("Welcome back. "));
        assertTrue(HomeView.greeting(null, false).startsWith("Welcome. "));
    }
}
