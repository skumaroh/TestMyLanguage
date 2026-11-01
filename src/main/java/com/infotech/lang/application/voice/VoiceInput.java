package com.infotech.lang.application.voice;

import com.infotech.lang.application.ai.SpeechToText;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A "Speak" button. The browser records a few seconds (started inside the click, as browsers require), turns it into a small
 * 16 kHz mono WAV and hands it here; Meta speech-to-text returns the words, which go to {@code onText} (for example into a text
 * area where the learner can edit them). Reusable for any block that lets the learner answer by voice. Nothing is stored here.
 */
@JsModule("./components/voice-recorder.js")
public class VoiceInput extends Div {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(VoiceInput.class);

    /** About 20 s of 16 kHz 16-bit mono audio is 640 KB; anything larger is refused. */
    static final int MAX_WAV_BYTES = 1_000_000;

    private final transient SpeechToText speech;
    private final String language;
    private final transient Consumer<String> onText;
    private boolean busy;

    public VoiceInput(SpeechToText speech, String language, Consumer<String> onText) {
        this.speech = speech;
        this.language = language;
        this.onText = onText;
        String name = "hindi".equalsIgnoreCase(language) ? "Hindi" : "Tamil";
        Button talk = new Button("🎤 Speak in " + name);
        Span status = new Span();
        status.getStyle().set("margin-left", "var(--lumo-space-s)");
        Span note = new Span("Your voice is sent to Meta to turn it into text. It is not saved.");
        note.getStyle().set("display", "block").set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");
        add(talk, status, note);
        addAttachListener(e -> getElement().executeJs("window.tmlVoice && window.tmlVoice.attach(this, $0, $1, $2)",
                talk.getElement(), status.getElement(), "🎤 Speak in " + name));
    }

    /** Called by the browser with the recording as base64 text. */
    @ClientCallable
    public void audioReady(String base64Wav) {
        if (busy) {
            return;
        }
        busy = true;
        String message = "";
        try {
            Optional<byte[]> wav = decodeWav(base64Wav);
            if (wav.isEmpty()) {
                message = "The recording could not be used. Please try again, or type your answer.";
            } else {
                SpeechToText.Result result = speech.transcribe(wav.get(), language);
                if (!result.ok()) {
                    message = "Speech to text is not available right now. Please type your answer.";
                } else if (result.text().isBlank()) {
                    message = "I could not hear any words. Please try again, or type your answer.";
                } else {
                    onText.accept(result.text());
                }
            }
        } catch (RuntimeException e) {
            logger.error("Voice input failed", e);
            message = "Something went wrong. Please type your answer.";
        } finally {
            busy = false;
            getElement().executeJs("this.__voiceDone && this.__voiceDone($0)", message);
        }
    }

    /** The WAV bytes when the text is a plausible, small 16-bit mono WAV file; otherwise empty. */
    static Optional<byte[]> decodeWav(String base64) {
        if (base64 == null || base64.isBlank() || base64.length() > MAX_WAV_BYTES * 4 / 3 + 8) {
            return Optional.empty();
        }
        byte[] wav;
        try {
            wav = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        boolean header = wav.length > 44 && wav.length <= MAX_WAV_BYTES
                && wav[0] == 'R' && wav[1] == 'I' && wav[2] == 'F' && wav[3] == 'F'
                && wav[8] == 'W' && wav[9] == 'A' && wav[10] == 'V' && wav[11] == 'E';
        if (!header) {
            return Optional.empty();
        }
        int channels = (wav[22] & 0xff) | ((wav[23] & 0xff) << 8);
        int rate = (wav[24] & 0xff) | ((wav[25] & 0xff) << 8) | ((wav[26] & 0xff) << 16);
        int bits = (wav[34] & 0xff) | ((wav[35] & 0xff) << 8);
        return channels == 1 && bits == 16 && (rate == 16000 || rate == 24000) ? Optional.of(wav) : Optional.empty();
    }
}
