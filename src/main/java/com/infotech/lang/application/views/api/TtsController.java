package com.infotech.lang.application.views.api;

import com.infotech.lang.application.voice.SpeechRegistry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/tts/{key}}: speech for a line a page registered ({@link SpeechRegistry}). Signed-in students only
 * ({@code SecurityConfig}). Unknown keys give 404, a speech service failure gives 502 (the page then uses the browser's voice).
 */
@RestController
@RequestMapping("/api/tts")
public class TtsController {

    private final SpeechRegistry registry;

    public TtsController(SpeechRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/{key:[0-9a-f]{32}}")
    public ResponseEntity<byte[]> get(@PathVariable String key) {
        if (!registry.isEnabled()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Optional<byte[]> audio = registry.audioFor(key);
        if (audio.isEmpty()) {
            return ResponseEntity.status(registry.isKnown(key) ? HttpStatus.BAD_GATEWAY : HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/wav"))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(audio.get());
    }
}
