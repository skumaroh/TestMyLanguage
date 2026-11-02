package com.infotech.lang.application.voice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.infotech.lang.application.ai.TextToSpeech;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SpeechRegistryTest {

    private static class FakeSpeech implements TextToSpeech {
        final AtomicInteger calls = new AtomicInteger();
        boolean configured = true;
        boolean works = true;

        @Override
        public boolean isConfigured() {
            return configured;
        }

        @Override
        public Result synthesize(String language, String text) {
            calls.incrementAndGet();
            return works ? Result.success(new byte[] {1, 2, 3}, 5) : Result.failure(5, "down");
        }
    }

    @Test
    void theKeyIsStableShortAndDependsOnLanguageAndText() {
        String a = SpeechRegistry.keyOf("tamil", "வணக்கம்");
        assertEquals(a, SpeechRegistry.keyOf("tamil", "வணக்கம்"));
        assertEquals(32, a.length());
        assertTrue(a.matches("[0-9a-f]{32}"));
        assertNotEquals(a, SpeechRegistry.keyOf("hindi", "வணக்கம்"));
        assertNotEquals(a, SpeechRegistry.keyOf("tamil", "நன்றி"));
    }

    @Test
    void aRegisteredLineIsSpokenOnceAndThenServedFromTheCache() {
        FakeSpeech speech = new FakeSpeech();
        SpeechRegistry registry = new SpeechRegistry(speech);
        String key = registry.register("tamil", "வணக்கம்");
        assertTrue(registry.isKnown(key));
        assertEquals(3, registry.audioFor(key).orElseThrow().length);
        assertEquals(3, registry.audioFor(key).orElseThrow().length);
        assertEquals(1, speech.calls.get());
    }

    @Test
    void anUnknownKeyIsNotSpokenAndAFailureGivesNothingAndIsNotCached() {
        FakeSpeech speech = new FakeSpeech();
        SpeechRegistry registry = new SpeechRegistry(speech);
        assertTrue(registry.audioFor("0123456789abcdef0123456789abcdef").isEmpty());
        assertEquals(0, speech.calls.get());
        speech.works = false;
        String key = registry.register("hindi", "नमस्ते");
        assertTrue(registry.audioFor(key).isEmpty());
        speech.works = true;
        assertTrue(registry.audioFor(key).isPresent());
        assertEquals(2, speech.calls.get());
    }

    @Test
    void disabledWhenNoServiceIsConfigured() {
        FakeSpeech speech = new FakeSpeech();
        speech.configured = false;
        assertFalse(new SpeechRegistry(speech).isEnabled());
    }

    @Test
    void theLineStoreForgetsTheOldestWhenFull() {
        SpeechRegistry registry = new SpeechRegistry(new FakeSpeech());
        String first = registry.register("tamil", "line 0");
        for (int i = 1; i <= SpeechRegistry.MAX_LINES; i++) {
            registry.register("tamil", "line " + i);
        }
        assertFalse(registry.isKnown(first));
    }
}
