package com.infotech.lang.application.voice;

import com.infotech.lang.application.ai.TextToSpeech;
import com.infotech.lang.application.ai.VoiceStyle;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Keeps the lines that Listen buttons may ask for, under a short key, so the text never travels in an address (it would end up in logs).
 * A page registers a line and gets the key for {@code /api/tts/{key}}; the controller looks the line up and asks the speech service.
 * Finished audio is kept for a while so a replay costs nothing. Both stores are small and in memory: after a restart the buttons simply
 * fall back to the browser's voice.
 */
@Component
public class SpeechRegistry {

    static final int MAX_LINES = 3000;
    static final int MAX_AUDIO = 150;

    /** A registered line. */
    public record Line(String language, String text, VoiceStyle style) {
    }

    private final TextToSpeech speech;
    private final Map<String, Line> lines = lruMap(MAX_LINES);
    private final Map<String, byte[]> audio = lruMap(MAX_AUDIO);

    public SpeechRegistry(TextToSpeech speech) {
        this.speech = speech;
    }

    public boolean isEnabled() {
        return speech.isConfigured();
    }

    /** The key under which {@code /api/tts/{key}} will speak this line. */
    public String register(String language, String text) {
        return register(language, text, VoiceStyle.BASE);
    }

    /** Same, with a coloured voice (pitch and speed); a different style gives a different key and its own cached audio. */
    public String register(String language, String text, VoiceStyle style) {
        VoiceStyle s = style == null ? VoiceStyle.BASE : style;
        String key = keyOf(language, text, s);
        synchronized (lines) {
            lines.put(key, new Line(language, text, s));
        }
        return key;
    }

    public boolean isKnown(String key) {
        synchronized (lines) {
            return lines.containsKey(key);
        }
    }

    /** The audio for a registered key (from the cache or freshly made), or empty if unknown or the service failed. */
    public Optional<byte[]> audioFor(String key) {
        Line line;
        synchronized (lines) {
            line = lines.get(key);
        }
        if (line == null) {
            return Optional.empty();
        }
        synchronized (audio) {
            byte[] cached = audio.get(key);
            if (cached != null) {
                return Optional.of(cached);
            }
        }
        TextToSpeech.Result result = speech.synthesize(line.language(), line.text(), line.style());
        if (!result.ok()) {
            return Optional.empty();
        }
        synchronized (audio) {
            audio.put(key, result.wav());
        }
        return Optional.of(result.wav());
    }

    static String keyOf(String language, String text) {
        return keyOf(language, text, VoiceStyle.BASE);
    }

    static String keyOf(String language, String text, VoiceStyle style) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            // the base voice hashes exactly as before, so existing keys do not change
            String voice = style == null || style.isBase() ? "" : "\u0000" + Math.round(style.pitch() * 10) + "\u0000" + Math.round(style.rate() * 100);
            byte[] digest = sha.digest((language + "\u0000" + text + voice).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static <V> Map<String, V> lruMap(int max) {
        return new LinkedHashMap<>(16, 0.75f, true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, V> eldest) {
                return size() > max;
            }
        };
    }
}
