package com.infotech.lang.application.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MetaSpeechToTextTest {

    private static MetaSpeechToText gateway(String key) {
        return new MetaSpeechToText(null, "https://api.meta.ai/v1", key, "muse-voice-transcribe-1.0", 5);
    }

    @Test
    void theRequestAsksForPushToTalkWavWithTheLanguageNameAsBias() {
        String json = gateway("k").requestJson("tamil");
        assertTrue(json.contains("\"mode\":\"PUSH_TO_TALK\""));
        assertTrue(json.contains("\"audioEncoding\":\"WAV\""));
        assertTrue(json.contains("\"languageBias\":[\"Tamil\"]"));
        assertTrue(gateway("k").requestJson("hindi").contains("[\"Hindi\"]"));
        assertTrue(gateway("k").requestJson(null).contains("[\"Tamil\"]"));
    }

    @Test
    void theMultipartBodyHasTheRequestAndTheAudio() throws Exception {
        byte[] wav = {1, 2, 3, 4};
        byte[] body = MetaSpeechToText.multipart("BOUND", "{\"a\":1}", wav);
        String text = new String(body, StandardCharsets.ISO_8859_1);
        assertTrue(text.contains("--BOUND\r\nContent-Disposition: form-data; name=\"request\""));
        assertTrue(text.contains("name=\"audio\"; filename=\"speech.wav\""));
        assertTrue(text.contains("Content-Type: audio/wav\r\n\r\n\u0001\u0002\u0003\u0004\r\n--BOUND--"));
    }

    @Test
    void readsTheTranscriptAndTreatsAMissingFieldAsFailure() {
        SpeechToText.Result ok = gateway("k").parse("{\"transcript\":\" मैं पानी पीता हूँ। \",\"turns\":[]}", 7);
        assertTrue(ok.ok());
        assertEquals("मैं पानी पीता हूँ।", ok.text());
        assertTrue(gateway("k").parse("{\"transcript\":\"\"}", 7).text().isEmpty());
        assertFalse(gateway("k").parse("{\"other\":1}", 7).ok());
        assertFalse(gateway("k").parse("not json", 7).ok());
    }

    @Test
    void withoutAKeyItFailsWithoutCallingOut() {
        // the call log is best effort, so a missing repository must not break the result
        SpeechToText.Result r = new MetaSpeechToText(null, "https://api.meta.ai/v1", " ", "m", 5).transcribe(new byte[10], "tamil");
        assertFalse(r.ok());
        assertEquals("model key not configured", r.error());
    }
}
