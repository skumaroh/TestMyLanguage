package com.infotech.lang.application.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MmsTextToSpeechTest {

    @Test
    void notConfiguredWithoutAnAddress() {
        assertFalse(new MmsTextToSpeech(null, "", 5).isConfigured());
        assertFalse(new MmsTextToSpeech(null, "   ", 5).isConfigured());
        assertTrue(new MmsTextToSpeech(null, "http://tts", 5).isConfigured());
    }

    @Test
    void withoutAnAddressOrTextItFailsWithoutCallingOut() {
        // the call log is best effort, so a missing repository must not break the result
        TextToSpeech.Result none = new MmsTextToSpeech(null, "", 5).synthesize("tamil", "வணக்கம்");
        assertFalse(none.ok());
        assertEquals("not configured", none.error());
        assertFalse(new MmsTextToSpeech(null, "http://127.0.0.1:1", 1).synthesize("tamil", "  ").ok());
    }

    @Test
    void anUnreachableServiceIsAFailureNotAnException() {
        TextToSpeech.Result r = new MmsTextToSpeech(null, "http://127.0.0.1:1", 1).synthesize("hindi", "नमस्ते");
        assertFalse(r.ok());
        assertEquals(0, r.wav().length);
    }

    @Test
    void anythingButHindiIsTamil() {
        assertEquals("hindi", MmsTextToSpeech.language("Hindi"));
        assertEquals("tamil", MmsTextToSpeech.language("tamil"));
        assertEquals("tamil", MmsTextToSpeech.language(null));
    }
}
