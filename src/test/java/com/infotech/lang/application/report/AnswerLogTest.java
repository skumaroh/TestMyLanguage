package com.infotech.lang.application.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AnswerLogTest {

    @Test
    void aBuiltWordHasNoMarkers() {
        AnswerLog.Parsed p = AnswerLog.parse("பூனை");
        assertEquals("பூனை", p.answer());
        assertEquals("", p.verdict());
        assertTrue(p.notes().isEmpty());
        assertFalse(p.skipped());
    }

    @Test
    void aTranslationWithTilesSpokenWordsAndASpellingNote() {
        AnswerLog.Parsed p = AnswerLog.parse("TILES நான் ஒரு மாணவன் [SAME_MEANING]");
        assertEquals("நான் ஒரு மாணவன்", p.answer());
        assertEquals("SAME_MEANING", p.verdict());
        assertEquals(java.util.List.of("Word tiles"), p.notes());

        AnswerLog.Parsed t = AnswerLog.parse("SPELLING-HELP 2 ப்ரவைகல் பரக்கும் [SAME_MEANING] {SPELLING MINOR_ERRORS: பறவைகள் பறக்கும்}");
        assertEquals("ப்ரவைகல் பரக்கும்", t.answer());
        assertEquals("பறவைகள் பறக்கும்", t.spellingCorrected());
        assertTrue(t.notes().contains("Used spelling help (2)"));
        assertTrue(t.notes().contains("A few spelling slips"));

        AnswerLog.Parsed v = AnswerLog.parse("VOICE-EDITED मैं पानी पीता हूँ [DIFFERENT_MEANING]");
        assertEquals("मैं पानी पीता हूँ", v.answer());
        assertEquals(java.util.List.of("Spoken, then edited"), v.notes());
    }

    @Test
    void aConversationTurnWithTheDialogueAndFlags() {
        AnswerLog.Parsed p = AnswerLog.parse("FLAG UNVERIFIED conversation: VOICE HINT வணக்கம் ராணி [MET: Greeted back.] "
                + "| ASKED: வணக்கம்! வாருங்கள். | NEXT: உங்களுக்கு என்ன வேண்டும்?");
        assertEquals("வணக்கம் ராணி", p.answer());
        assertEquals("MET", p.verdict());
        assertEquals("Greeted back.", p.reason());
        assertEquals("வணக்கம்! வாருங்கள்.", p.asked());
        assertEquals("உங்களுக்கு என்ன வேண்டும்?", p.next());
        assertTrue(p.notes().contains("Spoken"));
        assertTrue(p.notes().contains("Used \"Show English\""));
        assertTrue(p.notes().contains("This conversation was unverified"));
    }

    @Test
    void aSkippedQuestionAndAFailedChecker() {
        AnswerLog.Parsed s = AnswerLog.parse("[SKIPPED: TIMEOUT]");
        assertTrue(s.skipped());
        assertEquals("TIMEOUT", s.skipReason());
        AnswerLog.Parsed f = AnswerLog.parse("FLAG grader unavailable: ஏதோ [ERROR]");
        assertEquals("ஏதோ", f.answer());
        assertTrue(f.notes().get(0).contains("could not be reached"));
    }

    @Test
    void aDrawingShowsWhichLetterItLookedLikeButNoAnswerText() {
        AnswerLog.Parsed p = AnswerLog.parse("FLAG unclear: judged meant=அ best=ஆ score=0.812 lead=0.100 rank=2");
        assertEquals("", p.answer());
        assertTrue(p.notes().contains("The drawing was unclear"));
        assertTrue(p.notes().contains("Your drawing looked most like ஆ"));
    }

    @Test
    void nullAndEmptyAreSafe() {
        assertEquals("", AnswerLog.parse(null).answer());
        assertEquals("", AnswerLog.parse("   ").answer());
    }
}
