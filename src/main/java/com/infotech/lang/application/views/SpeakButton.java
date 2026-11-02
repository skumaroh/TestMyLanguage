package com.infotech.lang.application.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.infotech.lang.application.voice.SpeechRegistry;
import com.vaadin.flow.component.dependency.JsModule;

/**
 * A "Listen" button that plays a line aloud. With the speech service configured it plays Meta's open-source MMS voice (fetched from
 * {@code /api/tts/...} when pressed); otherwise, or if that fails, the browser's own voice. The click is handled in the browser so
 * playback starts inside the click. Where nothing can speak the language the button says so.
 */
@JsModule("./components/speak.js")
public class SpeakButton extends Button {

    private static final long serialVersionUID = 1L;

    /**
     * @param registry when its speech service is configured the line is played from Meta's MMS voice; the browser's own voice is the
     *                 fallback (and the only voice when the service is not configured or does not answer)
     */
    public SpeakButton(String text, String language, SpeechRegistry registry) {
        super("▶ Listen");
        addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        getElement().setAttribute("aria-label", "Listen to this line");
        boolean english = "english".equalsIgnoreCase(language);
        String tag = english ? "en-US" : "hindi".equalsIgnoreCase(language) ? "hi-IN" : "ta-IN";
        // the MMS voices are Tamil and Hindi only; English lines use the browser's voice
        String url = !english && registry != null && registry.isEnabled() ? "/api/tts/" + registry.register(language, text) : "";
        addAttachListener(e -> getElement().executeJs("window.tmlSpeak && window.tmlSpeak.attach(this, $0, $1, $2)", text, tag, url));
    }
}
