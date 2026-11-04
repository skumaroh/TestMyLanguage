package com.infotech.lang.application.voice;

import com.infotech.lang.application.ai.SpeechToText;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import java.util.Optional;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The "full audio" way of taking part in the conversation: the browser plays the persona's line, turns the microphone on, and when the
 * learner has spoken and then been quiet for two seconds sends the recording here. The words (Meta speech-to-text) go to {@code onHeard},
 * which answers exactly as if they had been typed; the next persona line is played with {@link #say}. The recording is not stored.
 */
@JsModule("./components/voice-conversation.js")
public class AudioConversation extends Div {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(AudioConversation.class);
    /** After this many misses in a row (nothing heard) the learner is sent back to the typing box. */
    static final int MAX_MISSES = 2;

    private final transient SpeechToText speech;
    private final String language;
    private final transient Consumer<String> onHeard;
    private final Runnable onStarted;
    private final Runnable onStopped;
    private int misses;
    private boolean busy;

    public AudioConversation(SpeechToText speech, String language, String personaName, String firstUrl, Runnable onStarted,
                             Consumer<String> onHeard, Runnable onStopped) {
        this.speech = speech;
        this.language = language;
        this.onHeard = onHeard;
        this.onStarted = onStarted;
        this.onStopped = onStopped;
        Button start = new Button("🎙 Talk with " + personaName + " (full audio)");
        start.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button stop = new Button("Stop talking, I will type instead");
        stop.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        stop.getStyle().set("display", "none");
        Div status = new Div();
        status.getStyle().set("font-size", "1.4em").set("font-weight", "600").set("min-height", "2.2em").set("margin", "var(--lumo-space-s) 0");
        Paragraph note = new Paragraph("The persona speaks first. When you finish talking, stay quiet for two seconds and your answer is sent. "
                + "Your voice is sent to Meta to turn it into text and is not saved. Please use made-up names and details.");
        note.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)").set("margin", "0");
        add(start, stop, status, note);
        getStyle().set("width", "100%").set("text-align", "center").set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-l)").set("padding", "var(--lumo-space-m)")
                .set("margin-bottom", "var(--lumo-space-m)").set("box-sizing", "border-box");
        addAttachListener(e -> getElement().executeJs("window.tmlTalk && window.tmlTalk.attach(this, $0, $1, $2, $3, $4)",
                start.getElement(), stop.getElement(), status.getElement(), personaName, firstUrl == null ? "" : firstUrl));
    }

    /** Plays the next persona line, then listens. */
    public void say(String audioUrl) {
        getElement().executeJs("this.__say && this.__say($0)", audioUrl == null ? "" : audioUrl);
    }

    /** Plays the last persona line, then ends the audio session. */
    public void sayLast(String audioUrl) {
        getElement().executeJs("this.__sayLast && this.__sayLast($0)", audioUrl == null ? "" : audioUrl);
    }

    @ClientCallable
    public void started() {
        misses = 0;
        onStarted.run();
    }

    @ClientCallable
    public void stopped() {
        onStopped.run();
    }

    @ClientCallable
    public void nothingHeard() {
        miss("I could not hear you. Please say it again.");
    }

    @ClientCallable
    public void audioReady(String base64Wav) {
        if (busy) {
            return;
        }
        busy = true;
        try {
            Optional<byte[]> wav = VoiceInput.decodeWav(base64Wav);
            if (wav.isEmpty()) {
                miss("The recording could not be used. Please say it again.");
                return;
            }
            SpeechToText.Result result = speech.transcribe(wav.get(), language);
            if (!result.ok()) {
                giveUp("Speech to text is not available right now. Please type your answers.");
            } else if (result.text().isBlank()) {
                miss("I could not hear any words. Please say it again.");
            } else {
                misses = 0;
                onHeard.accept(result.text());
            }
        } catch (RuntimeException e) {
            logger.error("Audio conversation turn failed", e);
            giveUp("Something went wrong. Please type your answers.");
        } finally {
            busy = false;
        }
    }

    private void miss(String message) {
        misses++;
        if (misses >= MAX_MISSES) {
            giveUp("I still could not hear you. Please type your answers instead.");
        } else {
            getElement().executeJs("this.__retry && this.__retry($0)", message);
        }
    }

    private void giveUp(String message) {
        getElement().executeJs("this.__giveUp && this.__giveUp($0)", message);
        onStopped.run();
    }
}
